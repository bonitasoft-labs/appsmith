import React from "react";
import styled from "styled-components";
import { DATASOURCE_REST_API_FORM } from "@appsmith/constants/forms";
import type { Datasource } from "entities/Datasource";
import type { InjectedFormProps } from "redux-form";
import { getFormMeta, reduxForm } from "redux-form";
import AnalyticsUtil from "utils/AnalyticsUtil";
import FormControl from "pages/Editor/FormControl";
import { StyledInfo } from "components/formControls/InputTextControl";
import { connect } from "react-redux";
import type { AppState } from "@appsmith/reducers";
import { Callout } from "design-system";
import {
  createDatasourceFromForm,
  toggleSaveActionFlag,
  updateDatasource,
} from "actions/datasourceActions";
import type { ReduxAction } from "@appsmith/constants/ReduxActionConstants";
import {
  datasourceToFormValues,
  formValuesToDatasource,
} from "transformers/RestAPIDatasourceFormTransformer";
import type {
  ApiDatasourceForm,
  AuthorizationCode,
  ClientCredentials,
} from "entities/Datasource/RestAPIForm";
import {
  ApiKeyAuthType,
  AuthType,
  GrantType,
} from "entities/Datasource/RestAPIForm";
import { createMessage, INVALID_URL } from "@appsmith/constants/messages";
import Collapsible from "./Collapsible";
import _ from "lodash";
import FormLabel from "components/editorComponents/FormLabel";
import CopyToClipBoard from "components/designSystems/appsmith/CopyToClipBoard";
import { updateReplayEntity } from "actions/pageActions";
import { ENTITY_TYPE } from "entities/AppsmithConsole";
import { TEMP_DATASOURCE_ID } from "constants/Datasource";
import { Form } from "./DBForm";
import { selectFeatureFlagCheck } from "@appsmith/selectors/featureFlagsSelectors";
import { getHasManageDatasourcePermission } from "@appsmith/utils/BusinessFeatures/permissionPageHelpers";
import { FEATURE_FLAG } from "@appsmith/entities/FeatureFlag";
import * as RestAPIDatasourceFormMembers from "./RestAPIDatasourceForm";
import RestAPIDatasourceForm from "./RestAPIDatasourceForm";


export class DatasourceBonitaEditor extends RestAPIDatasourceFormMembers.DatasourceRestAPIEditor {
    constructor(props: RestAPIDatasourceFormMembers.Props) {
      super(props);
    }

    renderGeneralSettings: () => JSX.Element= () => {
        this.props.formData.authType = AuthType.basic;
        return (
          <section
            className="t--section-general"
            data-location-id="section-General"
            data-testid="section-General"
          >
            <RestAPIDatasourceFormMembers.FormInputContainer data-location-id={btoa("url")}>
              {this.renderInputTextControlViaFormControl({
                configProperty: "url",
                label: "URL",
                placeholderText: "https://example.com",
                dataType: "TEXT",
                encrypted: false,
                isRequired: true,
                fieldValidator: this.urlValidator,
              })}
            </RestAPIDatasourceFormMembers.FormInputContainer>
            {this.renderBasic()}
          </section>
        );
    };

}

export default connect(
    RestAPIDatasourceFormMembers.mapStateToProps,
    RestAPIDatasourceFormMembers.mapDispatchToProps,
  )(
    reduxForm<ApiDatasourceForm, RestAPIDatasourceFormMembers.DatasourceRestApiEditorProps>({
      form: DATASOURCE_REST_API_FORM,
      enableReinitialize: true,
    })(DatasourceBonitaEditor),
  );