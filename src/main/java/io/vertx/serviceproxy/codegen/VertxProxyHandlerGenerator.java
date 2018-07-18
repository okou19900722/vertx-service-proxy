package io.vertx.serviceproxy.codegen;

import io.vertx.codegen.MethodInfo;
import io.vertx.codegen.ParamInfo;
import io.vertx.codegen.type.ApiTypeInfo;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.ParameterizedTypeInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.serviceproxy.model.ProxyMethodInfo;
import io.vertx.serviceproxy.model.ProxyModel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.vertx.codegen.type.ClassKind.*;

class VertxProxyHandlerGenerator extends AbstractServiceProxyGenerator {
  private static final Map<String, String> numericMapping = new HashMap<>();

  static {
    numericMapping.put("byte", "byte");
    numericMapping.put("java.lang.Byte", "byte");
    numericMapping.put("short", "short");
    numericMapping.put("java.lang.Short", "short");
    numericMapping.put("int", "int");
    numericMapping.put("java.lang.Integer", "int");
    numericMapping.put("long", "long");
    numericMapping.put("java.lang.Long", "long");
    numericMapping.put("float", "float");
    numericMapping.put("java.lang.Float", "float");
    numericMapping.put("double", "double");
    numericMapping.put("java.lang.Double", "double");
  }

  @Override
  public String filename(ProxyModel model) {
    return model.getIfaceFQCN() + "VertxProxyHandler.java";
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
    genComment(writer);
    writer.println("@SuppressWarnings({\"unchecked\", \"rawtypes\"})");
    String simpleName = model.getIfaceSimpleName();
    writer.print("public class ");
    writer.print(simpleName);
    writer.println("VertxProxyHandler extends ProxyHandler {");
    writer.println();
    writer.println("  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes ");
    writer.println();
    writer.println("  private final Vertx vertx;");
    writer.print("  private final ");
    writer.print(simpleName);
    writer.println(" service;");
    writer.println("  private final long timerID;");
    writer.println("  private long lastAccessed;");
    writer.println("  private final long timeoutSeconds;");
    writer.println();

    writer.print("  public ");
    writer.print(simpleName);
    writer.print("VertxProxyHandler(Vertx vertx, ");
    writer.print(simpleName);
    writer.println(" service) {");
    writer.println("    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);");
    writer.println("  }");
    writer.println();

    writer.print("  public ");
    writer.print(simpleName);
    writer.print("VertxProxyHandler(Vertx vertx, ");
    writer.print(simpleName);
    writer.println(" service, long timeoutInSecond) {");
    writer.println("    this(vertx, service, true, timeoutInSecond);");
    writer.println("  }");
    writer.println();

    writer.print("  public ");
    writer.print(simpleName);
    writer.print("VertxProxyHandler(Vertx vertx, ");
    writer.print(simpleName);
    writer.println(" service, boolean topLevel, long timeoutSeconds) {");
    writer.println("    this.vertx = vertx;");
    writer.println("    this.service = service;");
    writer.println("    this.timeoutSeconds = timeoutSeconds;");
    writer.println("    try {");
    writer.println("      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,");
    writer.println("          new ServiceExceptionMessageCodec());");
    writer.println("    } catch (IllegalStateException ex) {}");
    writer.println("    if (timeoutSeconds != -1 && !topLevel) {");
    writer.println("      long period = timeoutSeconds * 1000 / 2;");
    writer.println("      if (period > 10000) {");
    writer.println("        period = 10000;");
    writer.println("      }");
    writer.println("      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);");
    writer.println("    } else {");
    writer.println("      this.timerID = -1;");
    writer.println("    }");
    writer.println("    accessed();");
    writer.println("  }");
    writer.println();

    writer.println("  private void checkTimedOut(long id) {");
    writer.println("    long now = System.nanoTime();");
    writer.println("    if (now - lastAccessed > timeoutSeconds * 1000000000) {");
    for (MethodInfo m : model.getMethods()) {
      ProxyMethodInfo method = (ProxyMethodInfo) m;
      if (method.isProxyClose()) {
        if (method.getParams().isEmpty()) {
          writer.print("      service.");
          writer.print(method.getName());
          writer.println("();");
        } else {
          writer.print("      service.");
          writer.print(method.getName());
          writer.println("(done -> {});");
        }
      }
    }
    writer.println("      close();");
    writer.println("    }");
    writer.println("  }");
    writer.println();

    writer.println("  @Override");
    writer.println("  public void close() {");
    writer.println("    if (timerID != -1) {");
    writer.println("      vertx.cancelTimer(timerID);");
    writer.println("    }");
    writer.println("    super.close();");
    writer.println("  }");
    writer.println();

    writer.println("  private void accessed() {");
    writer.println("    this.lastAccessed = System.nanoTime();");
    writer.println("  }");
    writer.println();

    writer.println("  public void handle(Message<JsonObject> msg) {");
    writer.println("    try {");
    writer.println("      JsonObject json = msg.body();");
    writer.println("      String action = msg.headers().get(\"action\");");
    writer.println("      if (action == null) {");
    writer.println("        throw new IllegalStateException(\"action not specified\");");
    writer.println("      }");
    writer.println("      accessed();");
    writer.println("      switch (action) {");

    for (MethodInfo m : model.getMethods()) {
      ProxyMethodInfo method = (ProxyMethodInfo) m;
      if (!method.isStaticMethod()) {
        writer.print("        case \"");
        writer.print(method.getName());
        writer.println("\": {");
        writer.print("          service.");
        writer.print(method.getName());
        writer.print("(");
        boolean hasParams = !method.getParams().isEmpty();
        ParamInfo lastParam = hasParams ? method.getParams().get(method.getParams().size() - 1) : null;
        boolean hasResultHandler = lastParam != null && lastParam.getType().getKind() == ClassKind.HANDLER && ((ParameterizedTypeInfo) lastParam.getType()).getArg(0).getKind() == ClassKind.ASYNC_RESULT;
        TypeInfo type = !hasResultHandler ? null : ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) lastParam.getType()).getArg(0)).getArg(0);
        int count = 0;
        boolean first = true;
        for (ParamInfo param : method.getParams()) {
          if (first) {
            first = false;
          } else {
            writer.print(", ");
          }
          if (!hasResultHandler || count++ != method.getParams().size() - 1) {
            String paramTypeName = param.getType().getName();
            ClassKind kind = param.getType().getKind();
            if (paramTypeName.equals("char") || paramTypeName.equals("java.lang.Character")) {
              writer.print("json.getInteger(\"");
              writer.print(param.getName());
              writer.print("\") == null ? null : (char)(int)(json.getInteger(\"");
              writer.print(param.getName());
              writer.print("\"))");
            } else if (
              paramTypeName.equals("byte")
                || paramTypeName.equals("java.lang.Byte")
                || paramTypeName.equals("short")
                || paramTypeName.equals("java.lang.Short")
                || paramTypeName.equals("int")
                || paramTypeName.equals("java.lang.Integer")
                || paramTypeName.equals("long")
                || paramTypeName.equals("java.lang.Long")
              ) {
              writer.print("json.getValue(\"");
              writer.print(param.getName());
              writer.print("\") == null ? null : (json.getLong(\"");
              writer.print(param.getName());
              writer.print("\").");
              writer.print(numericMapping.get(paramTypeName));
              writer.print("Value())");
            } else if (
              paramTypeName.equals("float")
                || paramTypeName.equals("java.lang.Float")
                || paramTypeName.equals("double")
                || paramTypeName.equals("java.lang.Double")
              ) {
              writer.print("json.getValue(\"");
              writer.print(param.getName());
              writer.print("\") == null ? null : (json.getDouble(\"");
              writer.print(param.getName());
              writer.print("\").");
              writer.print(numericMapping.get(paramTypeName));
              writer.print("Value())");
            } else if (kind == ENUM) {
              writer.print("json.getString(\"");
              writer.print(param.getName());
              writer.print("\") == null ? null : ");
              writer.print(param.getType().getName());
              writer.print(".valueOf(json.getString(\"");
              writer.print(param.getName());
              writer.print("\"))");
            } else if (kind == LIST || kind == SET) {
              String collection = kind == LIST ? "List" : "Set";
              ParameterizedTypeInfo p = (ParameterizedTypeInfo) param.getType();
              TypeInfo arg = p.getArg(0);
              if (arg.getKind() == DATA_OBJECT) {
                writer.print("json.getJsonArray(\"");
                writer.print(param.getName());
                writer.print("\").stream().map(o -> new ");
                writer.print(arg.getSimpleName());
                writer.print("((JsonObject)o)).collect(Collectors.to");
                writer.print(collection);
                writer.print("())");
              } else if (arg.getName().equals("java.lang.Byte")
                || arg.getName().equals("java.lang.Short")
                || arg.getName().equals("java.lang.Integer")
                || arg.getName().equals("java.lang.Long")) {
                String abc = arg.getName();
                writer.print("json.getJsonArray(\"");
                writer.print(param.getName());
                writer.print("\").stream().map(o -> ((Number)o).");
                writer.print(numericMapping.get(abc));
                writer.print("Value()).collect(Collectors.to");
                writer.print(collection);
                writer.print("())");
              } else {
                writer.print("convert");
                writer.print(collection);
                writer.print("(json.getJsonArray(\"");
                writer.print(param.getName());
                writer.print("\").getList())");
              }
            } else if (kind == MAP) {
              ParameterizedTypeInfo p = (ParameterizedTypeInfo) param.getType();
              TypeInfo arg = p.getArg(1);
              String abc = arg.getName();
              if (abc.equals("java.lang.Byte")
                || abc.equals("java.lang.Short")
                || abc.equals("java.lang.Integer")
                || abc.equals("java.lang.Long")
                || abc.equals("java.lang.Float")
                || abc.equals("java.lang.Double")
                ) {
                writer.print("json.getJsonObject(\"");
                writer.print(param.getName());
                writer.print("\").getMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ((java.lang.Number)entry.getValue()).");
                writer.print(numericMapping.get(abc));
                writer.print("Value()))");
              } else {
                writer.print("convertMap(json.getJsonObject(\"");
                writer.print(param.getName());
                writer.print("\").getMap())");
              }
            } else if (kind == DATA_OBJECT) {
              writer.print("json.getJsonObject(\"");
              writer.print(param.getName());
              writer.print("\") == null ? null : new ");
              writer.print(param.getType().getName());
              writer.print("(json.getJsonObject(\"");
              writer.print(param.getName());
              writer.print("\"))");
            } else {
              writer.print("(");
              writer.print(param.getType().getName());
              writer.print(")json.getValue(\"");
              writer.print(param.getName());
              writer.print("\")");
            }
          } else if (type.getKind() == LIST && ((ParameterizedTypeInfo) type).getArg(0).getName().equals("java.lang.Character")) {
            writer.print("createListCharHandler(msg)");
          } else if (type.getKind() == LIST && ((ParameterizedTypeInfo) type).getArg(0).getKind() == DATA_OBJECT) {
            genServiceException(writer, simpleReply("new JsonArray(res.result().stream().map(r -> r == null ? null : r.toJson()).collect(Collectors.toList()))"));
          } else if (type.getKind() == LIST) {
            writer.print("createListHandler(msg)");
          } else if (type.getKind() == SET && ((ParameterizedTypeInfo) type).getArg(0).getName().equals("java.lang.Character")) {
            writer.print("createSetCharHandler(msg)");
          } else if (type.getKind() == SET && ((ParameterizedTypeInfo) type).getArg(0).getKind() == DATA_OBJECT) {
            genServiceException(writer, simpleReply("new JsonArray(res.result().stream().map(r -> r == null ? null : r.toJson()).collect(Collectors.toList()))"));
          } else if (type.getKind() == SET) {
            writer.print("createSetHandler(msg)");
          } else if (type.getKind() == DATA_OBJECT) {
            genServiceException(writer, simpleReply("res.result() == null ? null : res.result().toJson()"));
          } else //noinspection ConstantConditions
            if (type.getKind() == API && ((ApiTypeInfo) type).isProxyGen()) {
              writer.println("res -> {");
              writer.println("            if (res.failed()) {");
              writer.println("                if (res.cause() instanceof ServiceException) {");
              writer.println("                  msg.reply(res.cause());");
              writer.println("                } else {");
              writer.println("                  msg.reply(new ServiceException(-1, res.cause().getMessage()));");
              writer.println("                }");
              writer.println("            } else {");
              writer.println("              String proxyAddress = UUID.randomUUID().toString();");
              writer.print("              ProxyHelper.registerService(");
              writer.print(type.getSimpleName());
              writer.println(".class, vertx, res.result(), proxyAddress, false, timeoutSeconds);");
              writer.println("              msg.reply(null, new DeliveryOptions().addHeader(\"proxyaddr\", proxyAddress));");
              writer.println("            }");
              writer.print("          }");
            } else {
              writer.print("createHandler(msg)");
            }
        }
        writer.println(");");
        if (method.isProxyClose()) {
          writer.println("          close();");
        }
        writer.println("          break;");
        writer.print("        }");
      }
      writer.println();
    }

    writer.println("        default: {");
    writer.println("          throw new IllegalStateException(\"Invalid action: \" + action);");
    writer.println("        }");
    writer.println("      }");
    writer.println("    } catch (Throwable t) {");
    writer.println("      msg.reply(new ServiceException(500, t.getMessage()));");
    writer.println("      throw t;");
    writer.println("    }");
    writer.println("  }");
    writer.println();
    writer.println("  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {");
    writer.println("    return res -> {");
    writer.println("      if (res.failed()) {");
    writer.println("        if (res.cause() instanceof ServiceException) {");
    writer.println("          msg.reply(res.cause());");
    writer.println("        } else {");
    writer.println("          msg.reply(new ServiceException(-1, res.cause().getMessage()));");
    writer.println("        }");
    writer.println("      } else {");
    writer.println("        if (res.result() != null  && res.result().getClass().isEnum()) {");
    writer.println("          msg.reply(((Enum) res.result()).name());");
    writer.println("        } else {");
    writer.println("          msg.reply(res.result());");
    writer.println("        }");
    writer.println("      }");
    writer.println("    };");
    writer.println("  }");
    writer.println();
    writer.println("  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {");
    writer.println("    return res -> {");
    writer.println("      if (res.failed()) {");
    writer.println("        if (res.cause() instanceof ServiceException) {");
    writer.println("          msg.reply(res.cause());");
    writer.println("        } else {");
    writer.println("          msg.reply(new ServiceException(-1, res.cause().getMessage()));");
    writer.println("        }");
    writer.println("      } else {");
    writer.println("        msg.reply(new JsonArray(res.result()));");
    writer.println("      }");
    writer.println("    };");
    writer.println("  }");
    writer.println();
    writer.println("  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {");
    writer.println("    return res -> {");
    writer.println("      if (res.failed()) {");
    writer.println("        if (res.cause() instanceof ServiceException) {");
    writer.println("          msg.reply(res.cause());");
    writer.println("        } else {");
    writer.println("          msg.reply(new ServiceException(-1, res.cause().getMessage()));");
    writer.println("        }");
    writer.println("      } else {");
    writer.println("        msg.reply(new JsonArray(new ArrayList<>(res.result())));");
    writer.println("      }");
    writer.println("    };");
    writer.println("  }");
    writer.println();
    writer.println("  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {");
    writer.println("    return res -> {");
    writer.println("      if (res.failed()) {");
    writer.println("        if (res.cause() instanceof ServiceException) {");
    writer.println("          msg.reply(res.cause());");
    writer.println("        } else {");
    writer.println("          msg.reply(new ServiceException(-1, res.cause().getMessage()));");
    writer.println("        }");
    writer.println("      } else {");
    writer.println("        JsonArray arr = new JsonArray();");
    writer.println("        for (Character chr: res.result()) {");
    writer.println("          arr.add((int) chr);");
    writer.println("        }");
    writer.println("        msg.reply(arr);");
    writer.println("      }");
    writer.println("    };");
    writer.println("  }");
    writer.println();
    writer.println("  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {");
    writer.println("    return res -> {");
    writer.println("      if (res.failed()) {");
    writer.println("        if (res.cause() instanceof ServiceException) {");
    writer.println("          msg.reply(res.cause());");
    writer.println("        } else {");
    writer.println("          msg.reply(new ServiceException(-1, res.cause().getMessage()));");
    writer.println("        }");
    writer.println("      } else {");
    writer.println("        JsonArray arr = new JsonArray();");
    writer.println("        for (Character chr: res.result()) {");
    writer.println("          arr.add((int) chr);");
    writer.println("        }");
    writer.println("        msg.reply(arr);");
    writer.println("      }");
    writer.println("    };");
    writer.println("  }");
    writer.println();
    writer.println("  private <T> Map<String, T> convertMap(Map map) {");
    writer.println("    return (Map<String, T>)map;");
    writer.println("  }");
    writer.println();
    writer.println("  private <T> List<T> convertList(List list) {");
    writer.println("    return (List<T>)list;");
    writer.println("  }");
    writer.println();
    writer.println("  private <T> Set<T> convertSet(List list) {");
    writer.println("    return new HashSet<T>((List<T>)list);");
    writer.println("  }");
    writer.print("}");
    return sw.toString();
  }

  private Supplier<String> simpleReply(String reply) {
    return () -> "              msg.reply(" + reply + ");";
  }

  private void genServiceException(PrintWriter writer, Supplier<String> supplier) {
    writer.println("res -> {");
    writer.println("            if (res.failed()) {");
    writer.println("              if (res.cause() instanceof ServiceException) {");
    writer.println("                msg.reply(res.cause());");
    writer.println("              } else {");
    writer.println("                msg.reply(new ServiceException(-1, res.cause().getMessage()));");
    writer.println("              }");
    writer.println("            } else {");
    writer.println(supplier.get());
    writer.println("            }");
    writer.print("         }");
  }

  private void genImports(ProxyModel model, PrintWriter writer) {
    writer.print("import ");
    writer.print(model.getIfaceFQCN());
    writer.println(";");
    writer.println("import io.vertx.core.Vertx;");
    writer.println("import io.vertx.core.Handler;");
    writer.println("import io.vertx.core.AsyncResult;");
    writer.println("import io.vertx.core.eventbus.EventBus;");
    writer.println("import io.vertx.core.eventbus.Message;");
    writer.println("import io.vertx.core.eventbus.MessageConsumer;");
    writer.println("import io.vertx.core.eventbus.DeliveryOptions;");
    writer.println("import io.vertx.core.eventbus.ReplyException;");
    writer.println("import io.vertx.core.json.JsonObject;");
    writer.println("import io.vertx.core.json.JsonArray;");
    writer.println("import java.util.Collection;");
    writer.println("import java.util.ArrayList;");
    writer.println("import java.util.HashSet;");
    writer.println("import java.util.List;");
    writer.println("import java.util.Map;");
    writer.println("import java.util.Set;");
    writer.println("import java.util.UUID;");
    writer.println("import java.util.stream.Collectors;");
    writer.println("import io.vertx.serviceproxy.ProxyHelper;");
    writer.println("import io.vertx.serviceproxy.ProxyHandler;");
    writer.println("import io.vertx.serviceproxy.ServiceException;");
    writer.println("import io.vertx.serviceproxy.ServiceExceptionMessageCodec;");
    genImportedTypes(model, writer);
  }
}
