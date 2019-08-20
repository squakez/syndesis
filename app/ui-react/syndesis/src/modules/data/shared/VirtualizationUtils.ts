import {
  Connection,
  RestDataService,
  SchemaNode,
  SchemaNodeInfo,
  ViewDefinition,
  ViewInfo,
  VirtualizationPublishingDetails,
  VirtualizationSourceStatus,
} from '@syndesis/models';

const PREVIEW_VDB_NAME = 'PreviewVdb';
const SCHEMA_MODEL_SUFFIX = 'schemamodel';

export enum DvConnectionStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
}

export enum DvConnectionSelection {
  SELECTED = 'SELECTED',
  NOTSELECTED = 'NOTSELECTED',
}

/**
 * Get the name of the preview VDB used for preview queries
 */
export function getPreviewVdbName(): string {
  return PREVIEW_VDB_NAME;
}

/**
 * Recursively flattens the tree structure of SchemaNodes,
 * into an array of ViewInfos
 * @param viewInfos the array of ViewInfos
 * @param schemaNode the SchemaNode from which the ViewInfo is generated
 * @param nodePath path for current SchemaNode eg ['name0', 'name1', 'name2']
 * @param selectedViewNames names of views which are selected
 * @param existingViewNames names of views which exist (marked as update)
 */
export function generateViewInfos(
  viewInfos: ViewInfo[],
  schemaNode: SchemaNode,
  nodePath: string[],
  selectedViewNames: string[],
  existingViewNames: string[]
): void {
  if (schemaNode) {
    // Generate source path from nodePath array
    const sourcePath: string[] = [];
    for (const seg of nodePath) {
      sourcePath.push(seg);
    }

    // Creates ViewInfo if the SchemaNode is queryable
    if (schemaNode.queryable === true) {
      const vwName = schemaNode.name;
      // Determine whether ViewInfo should be selected
      const selectedState =
        selectedViewNames.findIndex(viewName => viewName === vwName) === -1
          ? false
          : true;
      // Deteremine whether ViewInfo is an update
      const hasExistingView =
        existingViewNames.findIndex(viewName => viewName === vwName) === -1
          ? false
          : true;
      // Create ViewInfo
      const view: ViewInfo = {
        connectionName: schemaNode.connectionName,
        isUpdate: hasExistingView,
        nodePath: sourcePath,
        selected: selectedState,
        viewName: vwName,
        viewSourceNode: schemaNode,
      };
      viewInfos.push(view);
    }
    // Update path for next level
    sourcePath.push(schemaNode.name);
    // Process this nodes children
    if (schemaNode.children && schemaNode.children.length > 0) {
      for (const childNode of schemaNode.children) {
        generateViewInfos(
          viewInfos,
          childNode,
          sourcePath,
          selectedViewNames,
          existingViewNames
        );
      }
    }
  }
}

/**
 * Recursively flattens the tree structure of SchemaNodes,
 * into an array of SchemaNodeInfos
 * @param schemaNodeInfos the array of SchemaNodeInfos
 * @param schemaNode the SchemaNode from which the SchemaNodeInfo is generated
 * @param nodePath path for current SchemaNode eg ['sName', 'tName']
 */
export function generateSchemaNodeInfos(
  schemaNodeInfos: SchemaNodeInfo[],
  schemaNode: SchemaNode,
  nodePath: string[]
): void {
  if (schemaNode) {
    // Generate source path from nodePath array
    const sourcePath: string[] = [];
    for (const seg of nodePath) {
      sourcePath.push(seg);
    }

    // Creates SchemaNodeInfo if the SchemaNode is queryable
    if (schemaNode.queryable === true) {
      // Create SchemaNodeInfo
      const view: SchemaNodeInfo = {
        connectionName: schemaNode.connectionName,
        name: schemaNode.name,
        nodePath: sourcePath,
        teiidName: schemaNode.teiidName
      };
      schemaNodeInfos.push(view);
    }
    // Update path for next level
    if(schemaNode.type !== 'root') {
      sourcePath.push(schemaNode.name);
    }
    // Process this nodes children
    if (schemaNode.children && schemaNode.children.length > 0) {
      for (const childNode of schemaNode.children) {
        generateSchemaNodeInfos(schemaNodeInfos, childNode, sourcePath);
      }
    }
  }
}

/**
 * Generate a ViewDefinition for the supplied info
 * @param schemaNodeInfo the SchemaNodeInfo for the view
 * @param dataVirtName the name of the virtualization
 * @param vwName the name for the view
 * @param vwDescription the (optional) description for the view
 */
export function generateViewDefinition(
  schemaNodeInfo: SchemaNodeInfo[],
  dataVirtName: string,
  vwName: string,
  vwDescription?: string
): ViewDefinition {
  const srcPaths: string[] = loadPaths(schemaNodeInfo);
  return getViewDefinition(
    vwName,
    dataVirtName,
    srcPaths,
    false,
    vwDescription
  );
}

function loadPaths(schemaNodeInfo: SchemaNodeInfo[]): string[] {
  const srcPaths: string[] = [];

  let index = 0;
  schemaNodeInfo.map(
    item =>
      (srcPaths[index++] = 'schema=' + item.connectionName + '/table=' + item.teiidName)
  );

  return srcPaths;
}

/**
 * Generate a ViewDefinition for the supplied values.
 * @param name the view name
 * @param dataVirtName the name of the virtualization
 * @param srcPaths paths for the sources used in the view
 * @param userDefined specifies if the ddl has been altered from defaults
 * @param description the (optional) view description
 * @param viewDdl the (optional) view DDL
 */
function getViewDefinition(
  name: string,
  dataVirtName: string,
  srcPaths: string[],
  userDefined: boolean,
  description?: string,
  viewDdl?: string
) {
  // View Definition
  const viewDefn: ViewDefinition = {
    dataVirtualizationName: dataVirtName,
    ddl: viewDdl ? viewDdl : '',
    id: '',
    isComplete: true,
    isUserDefined: userDefined,
    keng__description: description ? description : '',
    name,
    sourcePaths: srcPaths,
  };

  return viewDefn;
}

/**
 * Generate array of DvConnections.  Takes the incoming connections and updates the 'options',
 * based on the Virtualization connection status and selection state
 * @param conns the connections
 * @param virtualizationsSourceStatuses the available virtualization sources
 * @param selectedConn name of a selected connection
 * @param activeOnly (optional) true - return only active connections
 */
export function generateDvConnections(
  conns: Connection[],
  virtualizationsSourceStatuses: VirtualizationSourceStatus[],
  selectedConn: string,
  activeOnly = false
): Connection[] {
  const dvConns: Connection[] = [];
  for (const conn of conns) {
    let connStatus = DvConnectionStatus.INACTIVE;
    const virtSrcStatus = virtualizationsSourceStatuses.find(
      virtStatus => virtStatus.sourceName === conn.name
    );
    if (
      virtSrcStatus &&
      virtSrcStatus.vdbState === 'ACTIVE' &&
      virtSrcStatus.schemaState === 'ACTIVE'
    ) {
      connStatus = DvConnectionStatus.ACTIVE;
    }

    let selectionState = DvConnectionSelection.NOTSELECTED;
    if (conn.name === selectedConn) {
      selectionState = DvConnectionSelection.SELECTED;
    }
    conn.options = { dvStatus: connStatus, dvSelected: selectionState };
    if (!activeOnly) {
      dvConns.push(conn);
    } else if (connStatus === DvConnectionStatus.ACTIVE) {
      dvConns.push(conn);
    }
  }
  return dvConns;
}

/**
 * Get the Connection DV status.  DV uses the options on a connection to set status
 * @param connection the connection
 */
export function getDvConnectionStatus(conn: Connection): string {
  let dvState: string = DvConnectionStatus.INACTIVE;
  if (conn.options && conn.options.dvStatus) {
    dvState = conn.options.dvStatus;
  }
  return dvState;
}

/**
 * Determine if the Connection is selected with the DV wizard.  DV uses the options on a connection to set selection
 * @param connection the connection
 */
export function isDvConnectionSelected(conn: Connection) {
  let isSelected = false;
  if (
    conn.options &&
    conn.options.dvSelected &&
    conn.options.dvSelected === DvConnectionSelection.SELECTED
  ) {
    isSelected = true;
  }
  return isSelected;
}

/**
 * Get the OData url from the virtualization, if available
 * @param virtualization the RestDataService
 */
export function getOdataUrl(virtualization: RestDataService): string {
  return virtualization.odataHostName
    ? 'https://' + virtualization.odataHostName + '/odata'
    : '';
}

/**
 * Construct the pod build log url from the supplied info
 * @param consoleUrl the console url
 * @param namespace namespace of the DV pod
 * @param publishPodName name of the DV pod
 */
export function getPodLogUrl(
  consoleUrl: string,
  namespace?: string,
  publishPodName?: string
): string {
  return namespace && publishPodName ?
    `${consoleUrl}/project/${namespace}/browse/pods/${publishPodName}?tab=logs` :
    '';
}

/**
 * Get publishing state details for the specified virtualization
 * @param consoleUrl the console url
 * @param virtualization the RestDataService
 */
export function getPublishingDetails(
  consoleUrl: string,
  virtualization: RestDataService
): VirtualizationPublishingDetails {
  // Determine published state
  const publishStepDetails: VirtualizationPublishingDetails = {
    state: virtualization.publishedState,
    stepNumber: 0,
    stepText: '',
    stepTotal: 4,
  };
  switch (virtualization.publishedState) {
    case 'CONFIGURING':
      publishStepDetails.stepNumber = 1;
      publishStepDetails.stepText = 'Configuring';
      break;
    case 'BUILDING':
      publishStepDetails.stepNumber = 2;
      publishStepDetails.stepText = 'Building';
      break;
    case 'DEPLOYING':
      publishStepDetails.stepNumber = 3;
      publishStepDetails.stepText = 'Deploying';
      break;
    case 'RUNNING':
      publishStepDetails.stepNumber = 4;
      publishStepDetails.stepText = 'Published';
      break;
    default:
      break;
  }
  if (virtualization.publishPodName) {
    publishStepDetails.logUrl = getPodLogUrl(
      consoleUrl,
      virtualization.podNamespace,
      virtualization.publishPodName
    );
  }
  return publishStepDetails;
}

/**
 * Generate preview SQL for the specified view definition
 * @param viewDefinition the ViewDefinition
 */
export function getPreviewSql(viewDefinition: ViewDefinition): string {
  if (viewDefinition.ddl) {
    // Remove extra whitespaces, tabs and line feeds
    const trimmedSql: string = viewDefinition.ddl
      .replace(/\s+/g, ' ')
      .replace(/^\s|\s$/g, '');
    // Split the DDL string by the AS SELECT segment
    const ddlFragments = trimmedSql.split('AS SELECT ');
    // If the string array is > 1 prepend the remaining SQL statement to the SELECT
    if (ddlFragments.length > 1) {
      return 'SELECT ' + ddlFragments[1];
    }
    // TODO: More complex SQL may contain inner joins and SELECT statements, so we'll
    // need to expand this to more complicated cases.
  }

  // If no DDL is found then we assume a simple single source view
  // and use the select * from sourceTableName
  // TODO:  address multiple source tables
  const sourcePath = viewDefinition.sourcePaths[0];
  if (sourcePath) {
    return 'SELECT * FROM ' + getPreviewTableName(sourcePath) + ';';
  }
  return '';
}

/**
 * Get the table name for the preview query, given the source path.
 * Example sourcePath: (schema=pgConn/table=account)
 * @param sourcePath the path for the source
 */
function getPreviewTableName(sourcePath: string): string {
  const segments = sourcePath.split('/');
  const connectionName = segments[0].split('=')[1];
  const tableName = segments[1].split('=')[1];
  // Assemble the name, utilizing the schema model suffix
  return `"${connectionName.toLowerCase()}${SCHEMA_MODEL_SUFFIX}"."${tableName}"`;
}
