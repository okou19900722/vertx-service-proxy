package io.vertx.serviceproxy.codegen;

import io.vertx.serviceproxy.model.ProxyModel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

class VertxProxyHandlerGenerator extends AbstractServiceProxyGenerator {
  @Override
  public String filename(ProxyModel model) {
    return model.getIfaceFQCN() + "VertxProxyHandler.java";
  }

  @Override
  public String render(ProxyModel model, int index, int size, Map<String, Object> session) {
    StringWriter sw = new StringWriter();
    PrintWriter writer = new PrintWriter(sw);

    return sw.toString();
  }
}
