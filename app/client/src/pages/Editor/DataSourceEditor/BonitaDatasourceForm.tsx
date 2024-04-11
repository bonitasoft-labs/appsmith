import React from "react";
import { DATASOURCE_REST_API_FORM } from "@appsmith/constants/forms";
import { reduxForm } from "redux-form";
import { connect } from "react-redux";
import type { ApiDatasourceForm } from "entities/Datasource/RestAPIForm";
import { AuthType } from "entities/Datasource/RestAPIForm";
import * as RestAPIDatasourceFormMembers from "./RestAPIDatasourceForm";

export class DatasourceBonitaEditor extends RestAPIDatasourceFormMembers.DatasourceRestAPIEditor {
  constructor(props: RestAPIDatasourceFormMembers.Props) {
    super(props);
  }

  renderGeneralSettings: () => JSX.Element = () => {
    this.props.formData.authType = AuthType.basic;
    return (
      <section
        className="t--section-general"
        data-location-id="section-General"
        data-testid="section-General"
      >
        <RestAPIDatasourceFormMembers.FormInputContainer
          data-location-id={btoa("url")}
        >
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
  reduxForm<
    ApiDatasourceForm,
    RestAPIDatasourceFormMembers.DatasourceRestApiEditorProps
  >({
    form: DATASOURCE_REST_API_FORM,
    enableReinitialize: true,
  })(DatasourceBonitaEditor),
);
