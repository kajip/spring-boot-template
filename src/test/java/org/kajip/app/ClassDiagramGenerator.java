package org.kajip.app;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodParameterInfo;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Renders the compiled classes in a directory as a Mermaid class diagram in Markdown. */
final class ClassDiagramGenerator {

  private static final Set<String> RECORD_GENERATED_METHODS =
      Set.of("equals", "hashCode", "toString");

  private ClassDiagramGenerator() {}

  public static void main(String[] args) throws IOException {
    Path output = Path.of(args[1]);
    Files.createDirectories(output.getParent());
    Files.writeString(output, render(Path.of(args[0])));
  }

  static String render(Path classesDir) {
    try (ScanResult scan =
        new ClassGraph()
            .enableClasspathEntries(classesDir.toString())
            .enableClassInfo()
            .enableFieldInfo()
            .enableMethodInfo()
            .ignoreClassVisibility()
            .ignoreFieldVisibility()
            .ignoreMethodVisibility()
            .scan()) {
      List<ClassInfo> classes = scan.getAllClasses().stream().toList();
      StringBuilder diagram = new StringBuilder(256);
      diagram.append(
          "# クラス図\n\n`./gradlew generateDocs` で生成する。手で編集しない。\n\n```mermaid\nclassDiagram\n");
      classes.forEach(info -> appendClass(diagram, info));
      classes.forEach(info -> appendRelations(diagram, info, classes));
      return diagram.append("```\n").toString();
    }
  }

  private static void appendClass(StringBuilder diagram, ClassInfo info) {
    diagram.append("  class ").append(info.getSimpleName()).append(" {\n");
    if (info.isInterface()) {
      diagram.append("    <<interface>>\n");
    } else if (info.isRecord()) {
      diagram.append("    <<record>>\n");
    }
    for (FieldInfo field : info.getDeclaredFieldInfo()) {
      if (!field.isSynthetic()) {
        diagram
            .append("    ")
            .append(visibility(field.getModifiers()))
            .append(mermaidType(field.getTypeSignatureOrTypeDescriptor().toStringWithSimpleNames()))
            .append(' ')
            .append(field.getName())
            .append('\n');
      }
    }
    for (MethodInfo method : info.getDeclaredMethodInfo()) {
      if (!method.isSynthetic() && !isRecordGenerated(info, method)) {
        diagram
            .append("    ")
            .append(visibility(method.getModifiers()))
            .append(method.getName())
            .append('(')
            .append(parameters(method))
            .append(") ")
            .append(
                mermaidType(
                    method
                        .getTypeSignatureOrTypeDescriptor()
                        .getResultType()
                        .toStringWithSimpleNames()))
            .append('\n');
      }
    }
    diagram.append("  }\n");
  }

  private static void appendRelations(
      StringBuilder diagram, ClassInfo info, List<ClassInfo> classes) {
    if (info.getSuperclass() != null) {
      appendLine(diagram, info.getSuperclass().getSimpleName() + " <|-- " + info.getSimpleName());
    }
    for (ClassInfo parent : info.getDirectSuperinterfaces()) {
      appendLine(diagram, parent.getSimpleName() + " <|.. " + info.getSimpleName());
    }
    for (FieldInfo field : info.getDeclaredFieldInfo()) {
      String type = field.getTypeSignatureOrTypeDescriptor().toString();
      for (ClassInfo target : classes) {
        if (Pattern.compile("\\b" + Pattern.quote(target.getName()) + "\\b").matcher(type).find()) {
          appendLine(
              diagram,
              info.getSimpleName() + " --> " + target.getSimpleName() + " : " + field.getName());
        }
      }
    }
  }

  private static void appendLine(StringBuilder diagram, String line) {
    diagram.append("  ").append(line).append('\n');
  }

  private static boolean isRecordGenerated(ClassInfo info, MethodInfo method) {
    return info.isRecord() && RECORD_GENERATED_METHODS.contains(method.getName());
  }

  private static String parameters(MethodInfo method) {
    return method.getParameterInfo().stream()
        .map(MethodParameterInfo::getTypeSignatureOrTypeDescriptor)
        .map(type -> mermaidType(type.toStringWithSimpleNames()))
        .collect(Collectors.joining(", "));
  }

  private static String mermaidType(String javaType) {
    return javaType.replace('<', '~').replace('>', '~');
  }

  private static char visibility(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      return '+';
    }
    if (Modifier.isProtected(modifiers)) {
      return '#';
    }
    return Modifier.isPrivate(modifiers) ? '-' : '~';
  }
}
