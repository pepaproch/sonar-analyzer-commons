/*
 * SonarQube Analyzer Commons
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.analyzer.commons;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.check.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuleMetadataLoaderTest {

  private static final String RESOURCE_FOLDER = "org/sonarsource/analyzer/commons";
  private static final String DEFAULT_PROFILE_PATH = "org/sonarsource/analyzer/commons/Sonar_way_profile.json";
  private static final String RULE_REPOSITORY_KEY = "rule-definition-reader-test";
  private RulesDefinition.Context context;
  private NewRepository newRepository;
  private RuleMetadataLoader ruleMetadataLoader;

  @Before
  public void setup() {
    context = new RulesDefinition.Context();
    newRepository = context.createRepository(RULE_REPOSITORY_KEY, "magic");
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
  }

  @Test
  public void load_rule_S100() throws Exception {
    @Rule(key = "S100") class TestRule {
    }

    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Repository repository = context.repository(RULE_REPOSITORY_KEY);
    RulesDefinition.Rule rule = repository.rule("S100");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Function names should comply with a naming convention");
    assertThat(rule.htmlDescription()).isEqualTo("<p>description S100</p>");
    assertThat(rule.severity()).isEqualTo("MINOR");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.tags()).containsExactly("convention");
    DebtRemediationFunction remediation = rule.debtRemediationFunction();
    assertThat(remediation).isNotNull();
    assertThat(remediation.type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(remediation.baseEffort()).isEqualTo("5min");
  }

  @Test
  public void load_rule_S110() throws Exception {
    @Rule(key = "S110") class TestRule {
    }
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Repository repository = context.repository(RULE_REPOSITORY_KEY);
    RulesDefinition.Rule rule = repository.rule("S110");
    assertThat(rule).isNotNull();
    DebtRemediationFunction remediation = rule.debtRemediationFunction();
    assertThat(remediation).isNotNull();
    assertThat(remediation.type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(remediation.baseEffort()).isEqualTo("4h");
    assertThat(remediation.gapMultiplier()).isEqualTo("30min");
    assertThat(rule.gapDescription()).isEqualTo("Number of parents above the defined threshold");
  }

  @Test
  public void load_rules_key_based() throws Exception {
    ruleMetadataLoader.addRulesByRuleKey(newRepository, Arrays.asList("S110", "S100"));
    newRepository.done();

    RulesDefinition.Repository repository = context.repository(RULE_REPOSITORY_KEY);
    RulesDefinition.Rule ruleS110 = repository.rule("S110");
    assertThat(ruleS110).isNotNull();
    assertThat(ruleS110.name()).isEqualTo("Inheritance tree of classes should not be too deep");
    assertThat(ruleS110.htmlDescription()).isEqualTo("<p>description S110</p>");

    RulesDefinition.Rule ruleS100 = repository.rule("S100");
    assertThat(ruleS100).isNotNull();
    assertThat(ruleS100.name()).isEqualTo("Function names should comply with a naming convention");
    assertThat(ruleS100.htmlDescription()).isEqualTo("<p>description S100</p>");
  }

  @Test
  public void load_rule_S123() throws Exception {
    @Rule(key = "S123")
    class TestRule {
    }
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();

    RulesDefinition.Repository repository = context.repository(RULE_REPOSITORY_KEY);
    RulesDefinition.Rule rule = repository.rule("S123");
    assertThat(rule).isNotNull();
    DebtRemediationFunction remediation = rule.debtRemediationFunction();
    assertThat(remediation).isNotNull();
    assertThat(remediation.type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(remediation.gapMultiplier()).isEqualTo("10min");
    assertThat(rule.gapDescription()).isNull();
  }

  @Test
  public void load_rule_list() throws Exception {
    @Rule(key = "S100")
    class RuleA {
    }
    @Rule(key = "S110")
    class RuleB {
    }
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Arrays.asList(RuleA.class, RuleB.class));
    newRepository.done();
    RulesDefinition.Repository repository = context.repository(RULE_REPOSITORY_KEY);
    assertThat(repository.rule("S100")).isNotNull();
    assertThat(repository.rule("S110")).isNotNull();
  }

  @Test
  public void no_profile() throws Exception {
    @Rule(key = "S100")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Rule rule = context.repository(RULE_REPOSITORY_KEY).rule("S100");
    assertThat(rule.activatedByDefault()).isFalse();
  }

  @Test
  public void rule_not_in_default_profile() throws Exception {
    @Rule(key = "S123")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, DEFAULT_PROFILE_PATH);
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Rule rule = context.repository(RULE_REPOSITORY_KEY).rule("S123");
    assertThat(rule.activatedByDefault()).isFalse();
  }

  @Test
  public void rule_in_default_profile() throws Exception {
    @Rule(key = "S100")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, DEFAULT_PROFILE_PATH);
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Rule rule = context.repository(RULE_REPOSITORY_KEY).rule("S100");
    assertThat(rule.activatedByDefault()).isTrue();
  }

  @Test
  public void getStringArray() throws Exception {
    Map<String, Object> map = Collections.singletonMap("key", Arrays.asList("x", "y"));
    assertThat(RuleMetadataLoader.getStringArray(map, "key")).containsExactly("x", "y");
  }

  @Test(expected = IllegalStateException.class)
  public void getStringArray_with_invalid_type() throws Exception {
    Map<String, Object> map = Collections.singletonMap("key", "x");
    RuleMetadataLoader.getStringArray(map, "key");
  }

  @Test(expected = IllegalStateException.class)
  public void getStringArray_without_property() throws Exception {
    RuleMetadataLoader.getStringArray(Collections.emptyMap(), "key");
  }

  @Test
  public void test_security_hotspot() {
    @Rule(key = "S2092")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, DEFAULT_PROFILE_PATH, SonarVersion.SQ_73_RUNTIME);
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Rule rule = context.repository(RULE_REPOSITORY_KEY).rule("S2092");
    assertThat(rule.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    assertThat(rule.securityStandards()).containsExactlyInAnyOrder("cwe:311", "cwe:315", "cwe:614", "owaspTop10:a2", "owaspTop10:a3");
  }

  @Test
  public void test_security_hotspot_lts() {
    @Rule(key = "S2092")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, DEFAULT_PROFILE_PATH, SonarVersion.SQ_67_RUNTIME);
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Rule rule = context.repository(RULE_REPOSITORY_KEY).rule("S2092");
    assertThat(rule.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(rule.securityStandards()).hasSize(0);
  }

  @Test
  public void test_security_standards() {
    @Rule(key = "S112")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, DEFAULT_PROFILE_PATH, SonarVersion.SQ_73_RUNTIME);
    ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
    newRepository.done();
    RulesDefinition.Rule rule = context.repository(RULE_REPOSITORY_KEY).rule("S112");
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(rule.securityStandards()).containsExactlyInAnyOrder("cwe:397");
  }

  @Test
  public void test_invalid_json_string() {
    @Rule(key = "rule_missing_title")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
    try {
      ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
      fail("Should have failed");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e).hasMessage("Invalid property: title");
    }
  }

  @Test
  public void test_invalid_json_string_array() {
    @Rule(key = "rule_wrong_tag")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
    try {
      ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
      fail("Should have failed");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e).hasMessage("Invalid property: tags");
    }
  }

  @Test
  public void test_invalid_json_int_array() {
    @Rule(key = "rule_wrong_cwe")
    class TestRule {
    }
    ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, "org/sonarsource/analyzer/commons/profile_wrong_cwe.json", SonarVersion.SQ_73_RUNTIME);
    try {
      ruleMetadataLoader.addRulesByAnnotatedClass(newRepository, Collections.singletonList(TestRule.class));
      fail("Should have failed");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e).hasMessage("Invalid property: CWE");
    }
  }

}
