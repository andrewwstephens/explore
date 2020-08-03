import "resources/theme/semantic.less";
import "resources/less/style.less";
import "resources/less/components/visualization.less";
import "resources/less/vendor/aladin.less";
import "resources/css/charts.css";

import App from "sjs/targeteditor-fastopt.js";

if (module.hot) {
  module.hot.accept();
  App.TargetTest.runIOApp();
}
