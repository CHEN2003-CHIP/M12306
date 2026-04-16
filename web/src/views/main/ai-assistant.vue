<template>
  <div class="app-layout">
<!--    <div class="sidebar">-->
<!--      <div class="logo-section">-->
<!--        <img src="@/assets/logo.png" alt="硅谷小智" width="160" height="160" />-->
<!--        <span class="logo-text">USTC AI Assistant</span>-->
<!--      </div>-->
<!--      <a-button class="new-chat-button" @click="newChat">-->
<!--        <template #icon><i class="fa-solid fa-plus"></i></template>-->
<!--        新会话-->
<!--      </a-button>-->
<!--      <div class="model-selection">-->
<!--        <span class="model-title">选择模型</span>-->
<!--        <a-radio-group v-model:value="selectedModel" class="model-radio-group">-->
<!--          <a-radio value="xiaozhi">小智</a-radio>-->
<!--        </a-radio-group>-->
<!--      </div>-->
<!--    </div>-->
    <div class="main-content">
      <div class="chat-container">
        <div class="message-list" ref="messageListRef"></div>
      </div>
      <!-- 核心修改：改用原生HTML+Antd样式类，强制显示输入框/按钮 -->
      <div class="fixed-input-container">
        <!-- 原生textarea替代AInput，确保基础显示 -->
        <textarea
            v-model="inputMessage"
            placeholder="请输入消息"
            @keydown.enter.prevent="sendMessage"
            rows="3"
            class="ant-input"
            style="resize: none; border: 1px solid #d9d9d9 !important; background: #fff !important;"
        ></textarea>
        <!-- 原生button替代AButton，确保基础显示 -->
        <button
            @click="sendMessage"
            :disabled="isSending"
            class="ant-btn ant-btn-primary"
            style="width: 100px; height: 100%; background: #1890ff !important; color: #fff !important; border: none !important;"
        >
          {{ isSending ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import store from '@/store'
// import axios from 'axios'
import { v4 as uuidv4 } from 'uuid'
// 可选：如果仍想用Antd组件，确保全局注册或正确引入
// import { Button, Input, RadioGroup, Radio } from 'ant-design-vue'
// import 'ant-design-vue/dist/reset.css' // 关键：引入Antd基础样式

const messageListRef = ref(null)
const isSending = ref(false)
const uuid = ref('')
const inputMessage = ref('')
const messages = ref([])
const selectedModel = ref('xiaozhi')

const modelApiMap = {
  xiaozhi: '/assistant/chat',
}

onMounted(() => {
  initUUID()
  watch(messages, () => renderMessages(), { deep: true })
  hello().catch(err => {
    console.error('初始问候请求失败:', err)
    isSending.value = false
  })
})

const renderMessages = () => {
  if (!messageListRef.value) return
  messageListRef.value.innerHTML = ''
  messages.value.forEach(msg => {
    const msgEl = document.createElement('div')
    msgEl.className = msg.isUser ? 'message user-message' : 'message bot-message'
    msgEl.innerHTML = `
      <i class="${msg.isUser ? 'fa-solid fa-user' : 'fa-solid fa-robot'} message-icon"></i>
      <span>
        <span>${msg.content}</span>
        ${msg.isTyping ? '<span class="loading-dots"><span class="dot"></span><span class="dot"></span></span>' : ''}
      </span>
    `
    messageListRef.value.appendChild(msgEl)
  })
  scrollToBottom()
}

const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const hello = async () => {
  await sendRequest('你好')
}

const sendMessage = () => {
  if (inputMessage.value.trim()) {
    sendRequest(inputMessage.value.trim())
        .then(() => inputMessage.value = '')
        .catch(err => {
          console.error('发送消息失败:', err)
          isSending.value = false
        })
  }
}

// 修复：拼接完整的API地址（根据main.js的baseURL，或直接写死）
const getApiUrl = () => {
  // 优先用main.js中配置的axios baseURL，没有则写死
  const baseUrl = window.axios?.defaults?.baseURL || 'http://localhost:8000'
  return `${baseUrl}${modelApiMap[selectedModel.value]}`
}

// 修复：改用fetch处理SSE流式响应
const sendRequest = async (message) => {
  isSending.value = true

  try {
    // 1. 添加用户消息
    const userMsg = { isUser: true, content: message, isTyping: false }
    if (messages.value.length > 0) messages.value.push(userMsg)

    // 2. 添加机器人加载消息
    const botMsg = { isUser: false, content: '', isTyping: true }
    messages.value.push(botMsg)
    renderMessages()

    // 3. 构建请求参数
    const apiUrl = getApiUrl()
    const requestBody = JSON.stringify({
      memoryId: uuid.value,
      userMessage: message,
      token: store.state.member.token || ''
    })

    // 4. 发起fetch请求（适配SSE流式）
    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // 如果有token，从store取（参考main.js的axios拦截器）
        'token': store.state.member.token || ''
      },
      body: requestBody,
      credentials: 'include'  // 跨域带cookie（如果需要）
    })

    // 5. 校验请求是否成功
    if (!response.ok) {
      throw new Error(`请求失败：${response.status} ${response.statusText}`)
    }

    // 6. 解析SSE流式响应
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    const lastMsg = messages.value[messages.value.length - 1]
    // 新增：缓存未处理的剩余数据（解决跨块拼接问题）
    let remaining = '';

    // 循环读取流式数据
    let isReading=true
    while (isReading) {
      const { done, value } = await reader.read();
      if (done) {
        isReading = false; // 置为false退出循环
        break;
      }

      // 解码二进制数据为字符串
      const chunk = decoder.decode(value, { stream: true })
      // 分割SSE消息（处理多段消息拼接的情况）
      // 按SSE规范分割（\n\n是SSE消息分隔符）
      const messages = chunk.split(/\r?\n\r?\n/);
      // 最后一个元素可能是不完整的，缓存到下一轮
      remaining = messages.pop() || '';

      // 遍历所有完整的SSE消息
      for (const msg of messages) {
        if (!msg) continue;
        // 解析data字段（兼容data:xxx和data: xxx格式）
        const dataMatch = msg.match(/^data:\s*(.+)$/i);
        if (!dataMatch) continue;
        let data = dataMatch[1];

        // 处理结束标记
        if (data === '[DONE]') {
          lastMsg.isTyping = false;
          renderMessages();
          continue;
        }

        // 修复：还原转义的换行符
        data = data.replace(/\\n/g, '\n').replace(/\\r/g, '\r');
        // 拼接内容（关键：不要trim，保留所有字符）
        lastMsg.content += data;
        lastMsg.isTyping = false;
        renderMessages();

      }
    }
    // 处理最后剩余的未完成数据
    if (remaining) {
      const dataMatch = remaining.match(/^data:\s*(.+)$/i);
      if (dataMatch && dataMatch[1] !== '[DONE]') {
        let data = dataMatch[1].replace(/\\n/g, '\n').replace(/\\r/g, '\r');
        lastMsg.content += data;
        renderMessages();
      }
    }

    // 最终解码剩余数据
    decoder.decode()

  } catch (error) {
    // 错误处理
    const lastMsg = messages.value[messages.value.length - 1]
    lastMsg.content = `请求失败：${error.message || '网络错误/接口异常'}`
    lastMsg.isTyping = false
    renderMessages()
    throw error
  } finally {
    isSending.value = false
  }
}

// const sendRequest = async (message) => {
//   isSending.value = true
//
//   try {
//     const userMsg = { isUser: true, content: message, isTyping: false }
//     if (messages.value.length > 0) messages.value.push(userMsg)
//
//     const botMsg = { isUser: false, content: '', isTyping: true }
//     messages.value.push(botMsg)
//     renderMessages()
//
//     const apiUrl = modelApiMap[selectedModel.value]
//     const response = await axios.post(
//         apiUrl,
//         { memoryId: uuid.value, userMessage: message },
//         {
//           responseType: 'text',
//           onDownloadProgress: (e) => {
//             if (e.event.target.responseText) {
//               const fullText = e.event.target.responseText
//               const lastMsg = messages.value[messages.value.length - 1]
//               lastMsg.content = fullText
//               renderMessages()
//             }
//           },
//         }
//     )
//
//     const lastMsg = messages.value[messages.value.length - 1]
//     if (!lastMsg.content) {
//       lastMsg.content = response.data?.content || response.data || '已收到你的消息，但暂无回复'
//     }
//     lastMsg.isTyping = false
//   } catch (error) {
//     const lastMsg = messages.value[messages.value.length - 1]
//     lastMsg.content = `请求失败：${error.message || '网络错误/接口未找到'}`
//     lastMsg.isTyping = false
//     throw error
//   } finally {
//     isSending.value = false
//   }
// }

// const sendRequest = async (message) => {
//   isSending.value = true;
//
//   try {
//     const userMsg = { isUser: true, content: message, isTyping: false };
//     if (messages.value.length > 0) messages.value.push(userMsg);
//
//     const botMsg = { isUser: false, content: '', isTyping: true };
//     messages.value.push(botMsg);
//     renderMessages();
//
//     // 关键修改1：直接指定8000端口的完整绝对请求地址，抛弃相对路径
//     const requestUrl = 'http://localhost:8000/assistant/chat';
//     // 关键修改2：Fetch直接请求该绝对地址，其余配置不变
//     const response = await fetch(requestUrl, {
//       method: 'POST',
//       headers: {
//         'Content-Type': 'application/json', // 必须保留，保证后端能解析JSON参数
//       },
//       body: JSON.stringify({ memoryId: uuid.value, userMessage: message }),
//     });
//
//
//     // 校验请求是否成功（HTTP状态码200-299）
//     if (!response.ok) {
//       throw new Error(`接口请求失败，状态码：${response.status}`);
//     }
//
//     // 核心：获取流式响应的读取器，逐块读取数据
//     const reader = response.body.getReader();
//     const decoder = new TextDecoder('utf-8'); // 解码二进制流为UTF-8文本
//     const lastMsg = messages.value[messages.value.length - 1];
//
//     // 循环读取分块数据
//     let isReadingStream = true;
//     while (isReadingStream) {
//       const { done, value } = await reader.read();
//       if (done) {
//         isReadingStream = false; // 数据读取完毕，修改变量退出循环
//         break;
//       }
//       lastMsg.content += decoder.decode(value, { stream: true });
//       lastMsg.isTyping = false;
//       renderMessages();
//     }
//
//     // 最终解码剩余数据（防止遗漏）
//     lastMsg.content += decoder.decode();
//   } catch (error) {
//     const lastMsg = messages.value[messages.value.length - 1];
//     lastMsg.content = `请求失败：${error.message || '网络错误/流式接口异常'}`;
//     lastMsg.isTyping = false;
//     throw error;
//   } finally {
//     isSending.value = false;
//   }
// };

const initUUID = () => {
  let storedUUID = localStorage.getItem('user_uuid')
  if (!storedUUID) {
    storedUUID = uuidToNumber(uuidv4())
    localStorage.setItem('user_uuid', storedUUID)
  }
  uuid.value = storedUUID
}

const uuidToNumber = (uuid) => {
  let number = 0
  for (let i = 0; i < uuid.length && i < 6; i++) {
    number = number * 16 + (parseInt(uuid[i], 16) || 0)
  }
  return number % 1000000
}

// const newChat = () => {
//   localStorage.removeItem('user_uuid')
//   window.location.reload()
// }
</script>

<style>
/* 全局样式：必须放在scoped外 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body {
  height: 100%;
  overflow: hidden; /* 禁止全局滚动 */
}
/* 引入Antd基础样式（如果项目未全局引入） */
/* 若项目已安装ant-design-vue，取消下面注释 */
/* @import 'ant-design-vue/dist/reset.css'; */
</style>

<style scoped>
/* 整体布局 */
.app-layout {
  display: flex;
  height: 100vh;
  width: 80vw;
}

/* 侧边栏 */
.sidebar {
  width: 200px;
  background-color: #f4f4f9;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo-section {
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}
.logo-text {
  font-size: 18px;
  font-weight: bold;
  margin-top: 10px;
  color: #333;
}
.new-chat-button {
  width: 100%;
  margin-bottom: 20px;
}
.model-selection {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.model-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #555;
}
.model-radio-group {
  width: 100%;
  flex-direction: column;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 20px;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  padding-bottom: 190px;
  position: relative;
  /* 确保主内容区层级高于其他元素 */
  z-index: 1;
}

/* 消息列表 */
.chat-container {
  width: 100%;
  max-height: calc(100vh - 280px);
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow-y: auto;
}
.message-list {
  width: 100%;
  padding: 15px;
}
.message {
  margin-bottom: 12px;
  padding: 10px 15px;
  border-radius: 8px;
  display: flex;
  max-width: 80%;
}
.user-message {
  background-color: #e6f7ff;
  align-self: flex-end;
  flex-direction: row-reverse;
}
.bot-message {
  background-color: #f6ffed;
  align-self: flex-start;
}
.message-icon {
  margin: 0 10px;
  font-size: 1.2em;
  color: #666;
}

/* 加载动画 */
.loading-dots {
  padding-left: 8px;
}
.dot {
  display: inline-block;
  margin-left: 4px;
  width: 8px;
  height: 8px;
  background-color: #1890ff;
  border-radius: 50%;
  animation: pulse 1.2s infinite ease-in-out both;
}
.dot:nth-child(2) {
  animation-delay: -0.6s;
}
@keyframes pulse {
  0%, 100% { transform: scale(0.6); opacity: 0.4; }
  50% { transform: scale(1); opacity: 1; }
}

/* 输入区域：强制显示，优先级最高 */
.fixed-input-container {
  position: absolute;
  bottom: 100px;
  left: 20px;
  right: 20px;
  height: 80px;
  display: flex;
  gap: 10px;
  align-items: stretch;
  /* 强制显示容器，排除透明度/隐藏问题 */
  opacity: 1 !important;
  visibility: visible !important;
  z-index: 999 !important; /* 最高层级，避免被遮挡 */
}
/* 原生textarea样式：强制可见 */
.fixed-input-container textarea {
  flex: 1;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid #d9d9d9 !important;
  outline: none;
  background: #ffffff !important;
  color: #000 !important;
}
/* 原生按钮样式：强制可见 */
.fixed-input-container button {
  width: 100px;
  border-radius: 8px;
  border: none;
  background: #1890ff !important;
  color: #ffffff !important;
  font-size: 14px;
  cursor: pointer;
}
/* 禁用状态样式 */
.fixed-input-container button:disabled {
  background: #8cc5ff !important;
  cursor: not-allowed;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    padding: 10px 20px;
    align-items: center;
  }
  .logo-section {
    flex-direction: row;
    margin-bottom: 0;
  }
  .logo-section img {
    width: 40px;
    height: 40px;
    margin-right: 10px;
  }
  .logo-text {
    font-size: 16px;
    margin-top: 0;
  }
  .new-chat-button {
    width: auto;
    margin-bottom: 0;
    margin-right: 10px;
  }
  .model-selection {
    flex-direction: row;
    width: auto;
  }
  .model-title {
    margin-bottom: 0;
    margin-right: 8px;
    font-size: 14px;
  }
  .model-radio-group {
    flex-direction: row;
  }
  .main-content {
    padding-bottom: 200px;
  }
  .chat-container {
    max-height: calc(100vh - 300px);
  }
}
</style>
