import { ActionGroup, 
         Alert, 
         Button, 
         Card, 
         CardBody, 
         Form } from '@patternfly/react-core';
import * as React from 'react';

export interface IVirtualizationCreateValidationResult {
  message: string;
  type: 'danger' | 'success';
}

export interface IVirtualizationCreateFormProps {
  /**
   * The localized text for the cancel button.
   */
  i18nCancelLabel: string;

  /**
   * The localized text for the create button.
   */
  i18nCreateLabel: string;

  /**
   * `true` if work in progress and this form should disable user input.
   */
  isWorking: boolean;

  /**
   * Form level validationResults
   */
  validationResults: IVirtualizationCreateValidationResult[];

  /**
   * The callback fired when submitting the form.
   * @param e
   */
  handleSubmit: (e?: any) => void;

  /**
   * The callback for cancel.
   */
  onCancel: () => void;
}

export const VirtualizationCreateForm: React.FunctionComponent<
  IVirtualizationCreateFormProps
> = props => {
  return (
    <Card>
      <CardBody>
        <Form
          isHorizontal={true}
          data-testid={'virtualization-create-form'}
          onSubmit={props.handleSubmit}
        >
          {props.validationResults.map((e, idx) => (
            <Alert key={idx} title={''} variant={e.type}>
              {e.message}
            </Alert>
          ))}
          {props.children}
          <ActionGroup>
            <Button
              data-testid={'virtualization-create-form-save-button'}
              variant="primary"
              isDisabled={props.isWorking}
              onClick={props.handleSubmit}
            >
              {props.i18nCreateLabel}
            </Button>
            <Button
              data-testid={'virtualization-create-form-cancel-button'}
              variant="secondary"
              isDisabled={props.isWorking}
              onClick={props.onCancel}
            >
              {props.i18nCancelLabel}
            </Button>
          </ActionGroup>
        </Form>
      </CardBody>
    </Card>
  );
}
