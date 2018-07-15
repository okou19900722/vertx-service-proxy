package io.vertx.serviceproxy.codegen;

import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.TypeParamInfo;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.serviceproxy.model.ProxyMethodInfo;
import io.vertx.serviceproxy.model.ProxyModel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class VertxEBProxyGenerator extends AbstractServiceProxyGenerator{
  @Override
  public String filename(ProxyModel model) {
    return model.getIfaceFQCN() + "VertxEBProxy.java";
  }
  @Override
  public String render(ProxyModel model, int index, int size, Map<String, Object> session) {
    StringWriter sw = new StringWriter();
    PrintWriter writer = new PrintWriter(sw);
    generateLicense(writer);
    writer.print("package ");
    writer.print(model.getIfacePackageName());
    writer.println(";");
    writer.println();
    genImports(model, writer);
    writer.println();
    //TODO 添加星号
    writer.println("/*");
    writer.println("  Generated Proxy code - DO NOT EDIT");
    writer.println("  @author Roger the Robot");
    writer.println("*/");
    writer.println("@SuppressWarnings({\"unchecked\", \"rawtypes\"})");

    String simpleName = model.getIfaceSimpleName();
    writer.print("public class ");
    writer.print(simpleName);
    writer.print("VertxEBProxy implements ");
    writer.print(simpleName);
    writer.println(" {");
    writer.println();
    writer.println("  private Vertx _vertx;");
    writer.println("  private String _address;");
    writer.println("  private DeliveryOptions _options;");
    writer.println("  private boolean closed;");
    writer.println();
    writer.print("  public ");
    writer.print(simpleName);
    writer.println("VertxEBProxy(Vertx vertx, String address) {");
    writer.println("    this(vertx, address, null);");
    writer.println("  }");
    writer.println();
    writer.print("  public ");
    writer.print(simpleName);
    writer.println("VertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {");
    writer.println("    this._vertx = vertx;");
    writer.println("    this._address = address;");
    writer.println("    this._options = options;");
    writer.println("    try {");
    writer.println("      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,");
    writer.println("          new ServiceExceptionMessageCodec());");
    writer.println("    } catch (IllegalStateException ex) {}");
    writer.println("  }");
    writer.println();


    for (MethodInfo m : model.getMethods()) {
      ProxyMethodInfo method = (ProxyMethodInfo) m;
      if (!method.isStaticMethod()) {
        writer.println("  @Override");
        writer.print("  ");
        startMethodTemplate(method, writer);
        if (!method.isProxyIgnore()) {
          genMethodBody(method, writer);
        }
        if (method.isFluent()) {
          writer.println("    return this;");
        }
        writer.println("  }");
        writer.println();
      }
    }
    writer.println();
    //TODO 调用Helper方法
    writer.println("  private List<Character> convertToListChar(JsonArray arr) {");
    writer.println("    List<Character> list = new ArrayList<>();");
    writer.println("    for (Object obj: arr) {");
    writer.println("      Integer jobj = (Integer)obj;");
    writer.println("      list.add((char)(int)jobj);");
    writer.println("    }");
    writer.println("    return list;");
    writer.println("  }");
    writer.println();
    writer.println("  private Set<Character> convertToSetChar(JsonArray arr) {");
    writer.println("    Set<Character> set = new HashSet<>();");
    writer.println("    for (Object obj: arr) {");
    writer.println("      Integer jobj = (Integer)obj;");
    writer.println("      set.add((char)(int)jobj);");
    writer.println("    }");
    writer.println("    return set;");
    writer.println("  }");
    writer.println();
    writer.println("  private <T> Map<String, T> convertMap(Map map) {");
    writer.println("    if (map.isEmpty()) { ");
    writer.println("      return (Map<String, T>) map; ");
    writer.println("    } ");
    writer.println("     ");
    writer.println("    Object elem = map.values().stream().findFirst().get(); ");
    writer.println("    if (!(elem instanceof Map) && !(elem instanceof List)) { ");
    writer.println("      return (Map<String, T>) map; ");
    writer.println("    } else { ");
    writer.println("      Function<Object, T> converter; ");
    writer.println("      if (elem instanceof List) { ");
    writer.println("        converter = object -> (T) new JsonArray((List) object); ");
    writer.println("      } else { ");
    writer.println("        converter = object -> (T) new JsonObject((Map) object); ");
    writer.println("      } ");
    writer.println("      return ((Map<String, T>) map).entrySet() ");
    writer.println("       .stream() ");
    writer.println("       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); ");
    writer.println("    } ");
    writer.println("  }");
    writer.println("  private <T> List<T> convertList(List list) {");
    writer.println("    if (list.isEmpty()) { ");
    writer.println("          return (List<T>) list; ");
    writer.println("        } ");
    writer.println("     ");
    writer.println("    Object elem = list.get(0); ");
    writer.println("    if (!(elem instanceof Map) && !(elem instanceof List)) { ");
    writer.println("      return (List<T>) list; ");
    writer.println("    } else { ");
    writer.println("      Function<Object, T> converter; ");
    writer.println("      if (elem instanceof List) { ");
    writer.println("        converter = object -> (T) new JsonArray((List) object); ");
    writer.println("      } else { ");
    writer.println("        converter = object -> (T) new JsonObject((Map) object); ");
    writer.println("      } ");
    writer.println("      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); ");
    writer.println("    } ");
    writer.println("  }");
    writer.println("  private <T> Set<T> convertSet(List list) {");
    writer.println("    return new HashSet<T>(convertList(list));");
    writer.println("  }");
    writer.println("}");
    return sw.toString();
  }
  private void genMethodBody(ProxyMethodInfo method, PrintWriter writer){
    boolean hasParams = !method.getParams().isEmpty();
    ParamInfo lastParam = hasParams ? method.getParams().get(method.getParams().size() - 1) : null;
    boolean hasResultHandler = lastParam != null && lastParam.getType().getKind() == ClassKind.HANDLER && ((ParameterizedTypeInfo)lastParam.getType()).getArg(0).getKind() == ClassKind.ASYNC_RESULT;
    int count = 0;
    if (hasResultHandler) {
      writer.println("  if (closed) {");
      writer.print("    ");
      writer.print(lastParam.getName());
      writer.println(".handle(Future.failedFuture(new IllegalStateException(\"Proxy is closed\")));");
      if (method.isFluent()) {
        writer.println("    return this;");
      } else {
        writer.println("    return;");
      }
      writer.println("  }");
    } else {
      writer.println("  if (closed) {");
      writer.println("    throw new IllegalStateException(\"Proxy is closed\");");
      writer.println("  }");
    }
    if (method.isProxyClose()) {
      writer.println("  closed = true;");
    }
    writer.println("  JsonObject _json = new JsonObject();");
    for (ParamInfo param : method.getParams()) {
      if (!hasResultHandler || count++ != method.getParams().size() - 1) {
        String paramTypeName = param.getType().getName();
        ClassKind kind = param.getType().getKind();
        if (paramTypeName.equals("char")) {
          genJsonPut(param, writer, getCaseSupplier(param, false, "int"));
        } else if (paramTypeName.equals("java.lang.Character")) {
          genJsonPut(param, writer, getCaseSupplier(param, true, "int"));
        } else if (kind == ClassKind.ENUM) {
          genJsonPut(param, writer, getToStringSupplier(param, true));
        } else if (kind == ClassKind.LIST) {
          ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) param.getType();
          TypeInfo arg = parameterizedTypeInfo.getArg(0);
          ClassKind argKind = arg.getKind();
          if (argKind == ClassKind.DATA_OBJECT) {
            genJsonPut(param, writer, () -> {
              StringBuilder sb = new StringBuilder();
              sb.append("new JsonArray(").append(param.getName()).append(".stream().map(r -> r == null ? null : r.toJson()).collect(Collectors.toList())));");
              return sb.toString();
            });
          } else {
            genJsonPut(param, writer, () -> {
              StringBuilder sb = new StringBuilder();
              sb.append("new JsonArray(").append(param.getName()).append("));");
              return sb.toString();
            });
          }
        } else if (kind == ClassKind.SET) {
          ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) param.getType();
          TypeInfo arg = parameterizedTypeInfo.getArg(0);
          ClassKind argKind = arg.getKind();
          if (argKind == ClassKind.DATA_OBJECT) {
            genJsonPut(param, writer, () -> {
              StringBuilder sb = new StringBuilder();
              sb.append("new JsonArray(").append(param.getName()).append(".stream().map(r -> r == null ? null : r.toJson()).collect(Collectors.toList())));");
              return sb.toString();
            });
          } else {
            genJsonPut(param, writer, () -> {
              StringBuilder sb = new StringBuilder();
              sb.append("new JsonArray(new ArrayList<>(").append(param.getName()).append(")));");
              return sb.toString();
            });
          }
        } else if (kind == ClassKind.MAP) {
          genJsonPut(param, writer, () -> "new JsonObject(convertMap(" + param.getName() + "))");
        } else if (kind == ClassKind.DATA_OBJECT) {
          genJsonPut(param, writer, () -> param.getName() + " == null ? null : " + param.getName() + ".toJson()");
        } else {
          genJsonPut(param, writer, param::getName);
        }
      }
      writer.println("    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();");
      writer.print("    _deliveryOptions.addHeader(\"action\", \"");
      writer.print(method.getName());
      writer.println("\");");
      if (hasResultHandler) {
        TypeInfo resultType = ((ParameterizedTypeInfo)((ParameterizedTypeInfo) lastParam.getType()).getArg(0)).getArg(0);
        ClassKind resultKind = resultType.getKind();
        writer.print("    _vertx.eventBus().");
        if (resultKind == ClassKind.LIST || resultKind == ClassKind.SET) {
          writer.print("<JsonArray>");
        } else if (resultKind == ClassKind.DATA_OBJECT) {
          writer.print("<JsonObject>");
        } else if (resultKind == ClassKind.ENUM) {
          writer.print("<String>");
        } else {
          writer.print("<");
          writer.print(resultType.getSimpleName());
          writer.println(">");
        }
        writer.println("send(_address, _json, _deliveryOptions, res -> {");
        writer.println("      if (res.failed()) {");
        writer.print("        ");
        writer.print(lastParam.getName());
        writer.println(".handle(Future.failedFuture(res.cause()));");
        writer.println("      } else {");
        if (resultKind == ClassKind.LIST) {

        }
      }
    }
  }
  private void genJsonPut(ParamInfo param, PrintWriter writer, Supplier<String> supplier){
    String paramName = param.getName();
    writer.print("    _json.put(\"");
    writer.print(paramName);
    writer.print("\", ");
    supplier.get();
    writer.println(");");
  }
  private Supplier<String> getCaseSupplier(ParamInfo param, boolean nullable, String paramType){
    return () -> {
      StringBuilder sb = new StringBuilder();
      String paramName = param.getName();
      if (nullable) {
        sb.append(paramName).append(" == null ? null : ");
      }
      sb.append("(").append(paramType).append(")").append(paramName);
      return sb.toString();
    };
  }
  private Supplier<String> getToStringSupplier(ParamInfo param, boolean nullable){
    return () -> {
      StringBuilder sb = new StringBuilder();
      String paramName = param.getName();
      if (nullable) {
        sb.append(paramName).append(" == null ? null : ");
      }
      sb.append(paramName).append(".toString()");
      return sb.toString();
    };
  }
  private void startMethodTemplate(MethodInfo method, PrintWriter writer){
    writer.print("public ");
    if (method.getTypeParams().size() > 0) {
      writer.print(method.getTypeParams().stream().map(TypeParamInfo::getName).collect(Collectors.joining(", ", "<", ">")));
      writer.print(" ");
    }
    writer.print(method.getReturnType().getSimpleName());
    writer.print(" ");
    writer.print(method.getName());
    writer.print("(");
    writer.print(method.getParams().stream().map(param -> param.getType().getSimpleName() + " " + param.getName()).collect(Collectors.joining(", ")));
    writer.print(") {");
  }

  private void genImports(ProxyModel model, PrintWriter writer){
    writer.print("import ");
    writer.print(model.getIfaceFQCN());
    writer.println(";");
    writer.println("import io.vertx.core.eventbus.DeliveryOptions;");
    writer.println("import io.vertx.core.Vertx;");
    writer.println("import io.vertx.core.Future;");
    writer.println("import io.vertx.core.json.JsonObject;");
    writer.println("import io.vertx.core.json.JsonArray;");
    writer.println("import java.util.ArrayList;");
    writer.println("import java.util.HashSet;");
    writer.println("import java.util.List;");
    writer.println("import java.util.Map;");
    writer.println("import java.util.Set;");
    writer.println("import java.util.stream.Collectors;");
    writer.println("import java.util.function.Function;");
    writer.println("import io.vertx.serviceproxy.ProxyHelper;");
    writer.println("import io.vertx.serviceproxy.ServiceException;");
    writer.println("import io.vertx.serviceproxy.ServiceExceptionMessageCodec;");
    for (TypeInfo importedType : model.getImportedTypes()) {
      if (!importedType.getRaw().getPackageName().equals("java.lang")) {
        writer.print("import ");
        writer.print(importedType.toString());
        writer.println(";");
      }
    }
  }
}
