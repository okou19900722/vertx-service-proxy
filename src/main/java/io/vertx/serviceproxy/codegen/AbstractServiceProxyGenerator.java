package io.vertx.serviceproxy.codegen;

import io.vertx.codegen.Generator;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.serviceproxy.model.ProxyModel;

import java.io.PrintWriter;
import java.util.Collections;

abstract class AbstractServiceProxyGenerator extends Generator<ProxyModel> {
  AbstractServiceProxyGenerator() {
    this.name = "service_proxies";
    this.kinds = Collections.singleton("proxy");
  }

  void genComment(PrintWriter writer){
    //TODO 添加星号
    writer.println("/*");
    writer.println("  Generated Proxy code - DO NOT EDIT");
    writer.println("  @author Roger the Robot");
    writer.println("*/");
  }

  void genImportedTypes(ProxyModel model, PrintWriter writer) {
    for (TypeInfo importedType : model.getImportedTypes()) {
      if (!importedType.getRaw().getPackageName().equals("java.lang")) {
        writer.print("import ");
        writer.print(importedType.toString());
        writer.println(";");
      }
    }
  }

  void generateLicense(PrintWriter writer) {
    writer.println("/*");
    writer.println("* Copyright 2014 Red Hat, Inc.");
    writer.println("*");
    writer.println("* Red Hat licenses this file to you under the Apache License, version 2.0");
    writer.println("* (the \"License\"); you may not use this file except in compliance with the");
    writer.println("* License. You may obtain a copy of the License at:");
    writer.println("*");
    writer.println("* http://www.apache.org/licenses/LICENSE-2.0");
    writer.println("*");
    writer.println("* Unless required by applicable law or agreed to in writing, software");
    writer.println("* distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT");
    writer.println("* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the");
    writer.println("* License for the specific language governing permissions and limitations");
    writer.println("* under the License.");
    writer.println("*/");
    writer.println();
  }
//  protected void generateLicense(PrintWriter writer) {
//    writer.println("/*");
//    writer.println(" * Copyright 2014 Red Hat, Inc.");
//    writer.println(" *");
//    writer.println(" * Red Hat licenses this file to you under the Apache License, version 2.0");
//    writer.println(" * (the \"License\"); you may not use this file except in compliance with the");
//    writer.println(" * License.  You may obtain a copy of the License at:");
//    writer.println(" *");
//    writer.println(" * http://www.apache.org/licenses/LICENSE-2.0");
//    writer.println(" *");
//    writer.println(" * Unless required by applicable law or agreed to in writing, software");
//    writer.println(" * distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT");
//    writer.println(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the");
//    writer.println(" * License for the specific language governing permissions and limitations");
//    writer.println(" * under the License.");
//    writer.println(" */");
//    writer.println();
//  }
}
