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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ChatToolPrompt {
  @Value("${ai.robot-name}")
  private String robotName;
  @Value("${ai.team-name}")
  private String teamName;
  private static final String chatPrompt =
      """
          Your role is {role}, {role_introduction}, your name is {agent_name}, developed by the {team_name} team.
          The user's name is {user_name}
          Reference: Current time is {time}, Lunar date is {lunar_date}. {information}
          ## Current Memory
            The current concept of memory and its content: {memory_map}
            {current_memory}
          {knowledgeGraphicInject}
          ## User Question
          {question}
          ## Conversation Rules (MUST FOLLOW)
          1. CRITICAL: NEVER repeat, paraphrase, or echo the user's question or parts of it.
          2. Answer directly and naturally - start with the core response, not "好的" or "让我想想".
          3. Be conversational like chatting with a close friend - use natural, flowing language.
          4. Keep responses concise (30-60 words for simple questions, up to 100 for complex ones).
          5. Avoid repetitive sentence structures across multiple conversations.
          6. If uncertain, say so honestly rather than giving vague or generic responses.
          7. Don't start every response the same way - vary your opening phrases.
          8. Skip unnecessary pleasantries like "我很高兴为您解答" - get to the point.
          ## Output Format
          - No line breaks (\\n) in response
          - NO emojis
          - Speak naturally, avoid robotic or overly formal tone
          """;

  public String getChatTool(String assistantName, String userName, String role,
      String roleIntroduction, String memory, String information, String memoryMap,
      String knowledgeGraphicInject, String question) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String formattedDate = formatter.format(date);
    Map<String, String> params = new HashMap<>();
    if (assistantName != null)
      params.put("agent_name", assistantName);
    else
      params.put("agent_name", robotName);
    params.put("question", question);
    params.put("team_name", teamName);
    params.put("time", formattedDate);
    // ========== 农历时间 ==========
    String lunarDate = getLunarDateString(date);
    params.put("lunar_date", lunarDate);
    params.put("current_memory", memory);
    params.put("memory_map", memoryMap);
    if (knowledgeGraphicInject != null) {
      params.put("knowledgeGraphicInject", "## Knowledge Graphic \n" + knowledgeGraphicInject);
    } else {
      params.put("knowledgeGraphicInject", "");
    }
    if (!information.isBlank()) {
      params.put("information", "知识库:" + information);
    } else {
      params.put("information", "");
    }
    params.put("user_name", Objects.requireNonNullElse(userName, "user"));
    params.put("role", Objects.requireNonNullElse(role, "智能助手"));
    params.put("role_introduction", Objects.requireNonNullElse(roleIntroduction,
        "你是一个友好、自然的对话伙伴，擅长用简洁有趣的方式回答问题，像朋友一样交流，避免刻板的客服语气"));
    return StringUtils.formatString(chatPrompt, params);
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
