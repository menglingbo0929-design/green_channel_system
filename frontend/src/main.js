import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

// Element Plus 完整导入（登录页只需要表单、按钮、输入框、消息提示）
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// Element Plus 中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const app = createApp(App)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
