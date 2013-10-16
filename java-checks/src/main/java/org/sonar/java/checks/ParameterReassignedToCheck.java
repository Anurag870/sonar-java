/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Sets;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AssignmentExpressionTree;
import org.sonar.java.model.BaseTreeVisitor;
import org.sonar.java.model.CatchTree;
import org.sonar.java.model.IdentifierTree;
import org.sonar.java.model.JavaFileScanner;
import org.sonar.java.model.JavaFileScannerContext;
import org.sonar.java.model.MethodTree;
import org.sonar.java.model.Tree;
import org.sonar.java.model.VariableTree;

import java.util.Set;

@Rule(
  key = ParameterReassignedToCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ParameterReassignedToCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1226";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private final Set<String> variables = Sets.newHashSet();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    variables.clear();
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    for (VariableTree parameterTree : tree.parameters()) {
      variables.add(parameterTree.simpleName());
    }
    super.visitMethod(tree);
    for (VariableTree parameterTree : tree.parameters()) {
      variables.remove(parameterTree.simpleName());
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    variables.add(tree.parameter().simpleName());
    super.visitCatch(tree);
    variables.remove(tree.parameter().simpleName());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) tree.variable();
      if (variables.contains(identifier.name())) {
        context.addIssue(tree, ruleKey, "Introduce a new variable instead of reusing the parameter \"" + identifier.name() + "\".");
      }
    }
  }

}
