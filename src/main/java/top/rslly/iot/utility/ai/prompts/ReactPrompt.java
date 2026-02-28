/**
 * Copyright © 2023-2030 The ruanrongman Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.rslly.iot.utility.ai.prompts;


import cn.hutool.core.date.ChineseDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.ProductRoleEntity;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ReactPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  @Autowired
  @Lazy
  private DescriptionUtil descriptionUtil;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  private static final String ReactSystem =
      """
          Your role is {role}, {role_introduction},your name is {agent_name},developed by the {team_name} team.
          The user's name is {user_name}
          The current concept of memory and its content: {memory_map}
          The current time is {time}, Lunar date is {lunar_date}.
          As a diligent Task Agent, you goal is to effectively accomplish the provided task or question as best as you can.

          ## Tools
          You have access to the following tools, the tools information is provided by the following schema:
          {tool_descriptions}

          ## Output Format
          To answer the question, Use the following JSON format. JSON only, no explanation. Otherwise, you will be punished.
          The output should be formatted as a JSON instance that conforms to the format below. JSON only, no explanation.

          ```json
          {
          "thought": "The thought of what to do and why.",
          "action": # the action to take, must be one of provided tools
              {
              "name": "tool name",
              "args": "tool input parameters, json type data"
              }
          }
          ```

          If this format is used, the user will respond in the following format:

          ```
          Observation: tool response
          ```

          You can keep repeating the above format until you have sufficient information.
          Remember, you may attempt up to three times,but in almost all cases you should complete
          the task in a single attempt.Otherwise, you will be punished.
          You must provide a response,using the following format:

          ```json
          {
          "thought": "The thought of what to do and why.",
          "action": {
              "name": "finish",
              "args": {"content": "Answer in Chinese. If unable, explain why and summarize reasoning."}
              }
          }
          ```

          ## Attention
          - Your output is JSON only and no explanation.
          - Choose only ONE tool and you can't do without using any tools in one step.
          - Thought use Chinese,Your thoughts can be seen by users, please try to match the role as much as possible.
          - Your final answer and middle step output language should be consistent with the language used by the user.
          - Whether the action input is JSON or str depends on the definition of the tool.
          - Your final answer should match the role, and should be kept within 100 words as much as possible.
          - No emojis are allowed to be output.

          ## User question
          {question}

          ## Current Conversation
          Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getReact(String toolDescriptions, String question, int productId) {
    Map<String, String> params = new HashMap<>();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String formattedDate = formatter.format(date);
    params.put("time", formattedDate);
    // ========== 农历时间 ==========
    String lunarDate = getLunarDateString(date);
    params.put("lunar_date", lunarDate);
    params.put("memory_map", descriptionUtil.getAgentLongMemory(productId));
    params.put("tool_descriptions", toolDescriptions);
    params.put("question", question);
    List<ProductRoleEntity> productRole = productRoleService.findAllByProductId(productId);
    String assistantName = null;
    String userName = null;
    String role = null;
    String roleIntroduction = null;
    if (!productRole.isEmpty()) {
      assistantName = productRole.get(0).getAssistantName();
      userName = productRole.get(0).getUserName();
      role = productRole.get(0).getRole();
      roleIntroduction = productRole.get(0).getRoleIntroduction();
    }
    if (assistantName != null)
      params.put("agent_name", assistantName);
    else
      params.put("agent_name", robotName);
    params.put("team_name", teamName);
    params.put("user_name", Objects.requireNonNullElse(userName, "user"));
    params.put("role", Objects.requireNonNullElse(role, "smart speaker"));
    params.put("role_introduction", Objects.requireNonNullElse(roleIntroduction,
        "你可以回答新闻内容和用户的各种合法请求，你回答的每句话都尽量口语化、简短,总是喜欢使用表情符号"));
    return StringUtils.formatString(ReactSystem, params);
  }

  /**
   * 使用 Hutool 获取农历日期字符串
   *
   * @param date 公历日期
   * @return 农历日期字符串 例如：甲辰年腊月初五
   */
  private String getLunarDateString(Date date) {
    try {
      ChineseDate chineseDate = new ChineseDate(date);
      // 天干地支年份 + 生肖 + 月 + 日
      // 例如：甲辰年（龙年）腊月初五
      return chineseDate.getCyclical() // 甲辰年
          + "(" + chineseDate.getChineseZodiac() + "年)" // (龙年)
          + chineseDate.getChineseMonthName() // 腊月
          + chineseDate.getChineseDay() // 初五
          + chineseDate.getFestivals(); // 节日
    } catch (Exception e) {
      return "";
    }
  }
}
