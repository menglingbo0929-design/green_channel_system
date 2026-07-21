<template>
  <div class="base-data-page">
    <h1 class="page-title">基础数据管理</h1>
    <div class="card">
      <el-tabs v-model="activeTab">

        <!-- ================================================================ -->
        <!-- Tab 1：用户账号 -->
        <!-- ================================================================ -->
        <el-tab-pane label="用户账号" name="users">
          <div class="toolbar">
            <el-input v-model="userSearch" placeholder="搜索用户名" prefix-icon="Search" clearable class="search-input" />
            <el-button type="primary" @click="openUserCreate"><el-icon><Plus /></el-icon>新增用户</el-button>
          </div>
          <el-table :data="filteredUsers" stripe border class="data-table">
            <el-table-column prop="id" label="ID" width="64" align="center" />
            <el-table-column prop="loginName" label="用户名" width="140" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="角色" width="200">
              <template #default="{ row }"><el-tag v-for="r in row.roles" :key="r" size="small" class="tag">{{ roleLabel[r] || r }}</el-tag></template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }"><el-tag :type="row.deleted===0?'success':'danger'" size="small">{{ row.deleted===0?'启用':'停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <div class="op-btns">
                  <el-button type="primary" link size="small" @click="openUserEdit(row)">编辑</el-button>
                  <el-button :type="row.deleted===0?'warning':'success'" link size="small" @click="handleUserToggle(row)">{{ row.deleted===0?'停用':'启用' }}</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ================================================================ -->
        <!-- Tab 2：组织结构 -->
        <!-- ================================================================ -->
        <el-tab-pane label="组织结构" name="org">
          <el-tabs v-model="orgTab" type="card" class="org-tabs">
            <!-- 学院 -->
            <el-tab-pane label="学院管理" name="college">
              <div class="toolbar">
                <span class="toolbar-hint">共 {{ colleges.length }} 个学院</span>
                <el-button type="primary" @click="openOrgDialog('college')"><el-icon><Plus /></el-icon>新增学院</el-button>
              </div>
              <el-table :data="colleges" stripe border class="data-table">
                <el-table-column prop="id" label="ID" width="64" align="center" />
                <el-table-column prop="collegeCode" label="编码" width="80" />
                <el-table-column prop="collegeName" label="学院名称" min-width="180" />
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }"><el-tag :type="row.enabled?'success':'danger'" size="small">{{ row.enabled?'启用':'停用' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template #default="{ row }">
                    <div class="op-btns">
                      <el-button type="primary" link size="small" @click="openOrgDialog('college', row)">编辑</el-button>
                    <el-button :type="row.enabled?'warning':'success'" link size="small" @click="handleOrgToggle('college',row)">{{ row.enabled?'停用':'启用' }}</el-button>
                  </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 专业 -->
            <el-tab-pane label="专业管理" name="major">
              <div class="toolbar">
                <el-select v-model="majorFilter" placeholder="筛选学院" clearable class="filter-select" @change="loadMajors">
                  <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
                </el-select>
                <el-button type="primary" @click="openOrgDialog('major')"><el-icon><Plus /></el-icon>新增专业</el-button>
              </div>
              <el-table :data="majors" stripe border class="data-table">
                <el-table-column prop="id" label="ID" width="64" align="center" />
                <el-table-column prop="majorCode" label="编码" width="80" />
                <el-table-column prop="majorName" label="专业名称" min-width="160" />
                <el-table-column label="所属学院" width="160">
                  <template #default="{ row }">{{ getCollegeName(row.collegeId) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }"><el-tag :type="row.enabled?'success':'danger'" size="small">{{ row.enabled?'启用':'停用' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template #default="{ row }">
                    <div class="op-btns">
                      <el-button type="primary" link size="small" @click="openOrgDialog('major', row)">编辑</el-button>
                    <el-button :type="row.enabled?'warning':'success'" link size="small" @click="handleOrgToggle('major',row)">{{ row.enabled?'停用':'启用' }}</el-button>
                  </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 年级 -->
            <el-tab-pane label="年级管理" name="grade">
              <div class="toolbar">
                <span class="toolbar-hint">共 {{ grades.length }} 个年级</span>
                <el-button type="primary" @click="openOrgDialog('grade')"><el-icon><Plus /></el-icon>新增年级</el-button>
              </div>
              <el-table :data="grades" stripe border class="data-table">
                <el-table-column prop="id" label="ID" width="64" align="center" />
                <el-table-column prop="gradeCode" label="编码" width="80" />
                <el-table-column prop="gradeName" label="年级名称" min-width="120" />
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }"><el-tag :type="row.enabled?'success':'danger'" size="small">{{ row.enabled?'启用':'停用' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template #default="{ row }">
                    <div class="op-btns">
                      <el-button type="primary" link size="small" @click="openOrgDialog('grade', row)">编辑</el-button>
                    <el-button :type="row.enabled?'warning':'success'" link size="small" @click="handleOrgToggle('grade',row)">{{ row.enabled?'停用':'启用' }}</el-button>
                  </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 班级 -->
            <el-tab-pane label="班级管理" name="class">
              <div class="toolbar">
                <el-select v-model="classFilter.collegeId" placeholder="学院" clearable class="filter-select" @change="loadClasses">
                  <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
                </el-select>
                <el-select v-model="classFilter.gradeId" placeholder="年级" clearable class="filter-select" @change="loadClasses">
                  <el-option v-for="g in grades" :key="g.id" :label="g.gradeName" :value="g.id" />
                </el-select>
                <el-button type="primary" @click="openOrgDialog('class')"><el-icon><Plus /></el-icon>新增班级</el-button>
              </div>
              <el-table :data="classes" stripe border class="data-table">
                <el-table-column prop="id" label="ID" width="64" align="center" />
                <el-table-column prop="classCode" label="编码" width="80" />
                <el-table-column prop="className" label="班级名称" min-width="120" />
                <el-table-column label="所属学院" width="160">
                  <template #default="{ row }">{{ getCollegeName(row.collegeId) }}</template>
                </el-table-column>
                <el-table-column label="所属年级" width="90">
                  <template #default="{ row }">{{ getGradeName(row.gradeId) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }"><el-tag :type="row.enabled?'success':'danger'" size="small">{{ row.enabled?'启用':'停用' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template #default="{ row }">
                    <div class="op-btns">
                      <el-button type="primary" link size="small" @click="openOrgDialog('class', row)">编辑</el-button>
                    <el-button :type="row.enabled?'warning':'success'" link size="small" @click="handleOrgToggle('class',row)">{{ row.enabled?'停用':'启用' }}</el-button>
                  </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>

        <!-- ================================================================ -->
        <!-- Tab 3：新生信息 -->
        <!-- ================================================================ -->
        <el-tab-pane label="新生信息" name="students">
          <!-- 筛选区 -->
          <div class="toolbar">
            <div class="filter-row">
              <el-input v-model="stuFilter.studentNo" placeholder="学号" clearable class="filter-input" @change="loadStudents" />
              <el-input v-model="stuFilter.studentName" placeholder="姓名" clearable class="filter-input" @change="loadStudents" />
              <el-select v-model="stuFilter.collegeId" placeholder="学院" clearable class="filter-input" @change="onCollegeChange">
                <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
              </el-select>
              <el-select v-model="stuFilter.majorId" placeholder="专业" clearable class="filter-input" @change="loadStudents" :disabled="!stuFilter.collegeId">
                <el-option v-for="m in stuMajors" :key="m.id" :label="m.majorName" :value="m.id" />
              </el-select>
              <el-select v-model="stuFilter.gradeId" placeholder="年级" clearable class="filter-input" @change="loadStudents">
                <el-option v-for="g in grades" :key="g.id" :label="g.gradeName" :value="g.id" />
              </el-select>
              <el-button type="primary" @click="loadStudents">查询</el-button>
              <el-button @click="resetStuFilter">重置</el-button>
            </div>
            <div class="filter-right">
              <el-button @click="downloadTemplate">下载模板</el-button>
              <el-button type="warning" @click="triggerUpload">批量导入</el-button>
              <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="handleUpload" />
              <el-button type="primary" @click="openStuDialog()"><el-icon><Plus /></el-icon>新增学生</el-button>
            </div>
          </div>

          <!-- 表格 -->
          <el-table :data="students" stripe border class="data-table">
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="100" />
            <el-table-column label="学院" width="140"><template #default="{row}">{{ getCollegeName(row.collegeId) }}</template></el-table-column>
            <el-table-column label="专业" width="140"><template #default="{row}">{{ getMajorName(row.majorId) }}</template></el-table-column>
            <el-table-column label="年级" width="80"><template #default="{row}">{{ getGradeName(row.gradeId) }}</template></el-table-column>
            <el-table-column label="班级" width="100"><template #default="{row}">{{ getClassName(row.classId) }}</template></el-table-column>
            <el-table-column prop="phone" label="联系电话" width="120" />
            <el-table-column label="生源地贷款" width="100" align="center">
              <template #default="{row}"><el-tag :type="row.originLoan?'warning':'info'" size="small">{{ row.originLoan?'有':'无' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="校园地贷款" width="110" align="center">
              <template #default="{row}"><el-tag :type="row.campusLoan?'warning':'info'" size="small">{{ row.campusLoan?'是':'否' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="difficultyLevel" label="困难等级" width="90" />
            <el-table-column label="信息完善" width="90" align="center">
              <template #default="{row}"><el-tag :type="row.infoComplete?'success':'danger'" size="small">{{ row.infoComplete?'是':'否' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{row}"><el-tag :type="row.enabled?'success':'danger'" size="small">{{ row.enabled?'启用':'停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{row}">
                <div class="op-btns">
                  <el-button type="primary" link size="small" @click="openStuDialog(row)">编辑</el-button>
                  <el-button :type="row.enabled?'warning':'success'" link size="small" @click="handleStuToggle(row)">{{ row.enabled?'停用':'启用' }}</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ============== 弹窗：用户新增/编辑 ============== -->
    <FormDialog v-model:visible="userDialog" :title="userIsEdit?'编辑用户':'新增用户'" :formData="userForm" :rules="userRules" :loading="userSaving" @submit="handleUserSave">
      <el-form-item label="用户名" prop="loginName"><el-input v-model="userForm.loginName" placeholder="请输入用户名" maxlength="32" /></el-form-item>
      <el-form-item v-if="!userIsEdit" label="密码" prop="password"><el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password /></el-form-item>
      <el-form-item label="角色" prop="roleIds">
        <el-checkbox-group v-model="userForm.roleIds"><el-checkbox v-for="r in roleOptions" :key="r.id" :label="r.id">{{ r.roleName }}</el-checkbox></el-checkbox-group>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="userForm.remark" type="textarea" :rows="2" placeholder="可选备注" /></el-form-item>
    </FormDialog>

    <!-- ============== 弹窗：学生新增/编辑 ============== -->
    <FormDialog v-model:visible="stuDialog" :title="stuIsEdit?'编辑学生':'新增学生'" :formData="stuForm" :rules="stuRules" :loading="stuSaving" @submit="handleStuSave">
      <el-form-item label="学号" prop="studentNo"><el-input v-model="stuForm.studentNo" placeholder="请输入学号" /></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="stuForm.studentName" placeholder="请输入姓名" /></el-form-item>
      <el-form-item label="学院" prop="collegeId">
        <el-select v-model="stuForm.collegeId" placeholder="请选择学院" @change="onStuCollegeChange"><el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" /></el-select>
      </el-form-item>
      <el-form-item label="专业" prop="majorId">
        <el-select v-model="stuForm.majorId" placeholder="请选择专业" :disabled="!stuForm.collegeId"><el-option v-for="m in stuFormMajors" :key="m.id" :label="m.majorName" :value="m.id" /></el-select>
      </el-form-item>
      <el-form-item label="年级" prop="gradeId">
        <el-select v-model="stuForm.gradeId" placeholder="请选择年级"><el-option v-for="g in grades" :key="g.id" :label="g.gradeName" :value="g.id" /></el-select>
      </el-form-item>
      <el-form-item label="班级" prop="classId">
        <el-select v-model="stuForm.classId" placeholder="请选择班级" :disabled="!stuForm.collegeId||!stuForm.gradeId"><el-option v-for="c in stuFormClasses" :key="c.id" :label="c.className" :value="c.id" /></el-select>
      </el-form-item>
      <el-form-item label="联系电话"><el-input v-model="stuForm.phone" placeholder="手机号" /></el-form-item>
      <el-form-item label="生源地贷款">
        <el-switch v-model="stuForm.originLoan" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="拟申请校园地贷款">
        <el-switch v-model="stuForm.campusLoan" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="困难等级">
        <el-select v-model="stuForm.difficultyLevel" placeholder="请选择" clearable>
          <el-option label="特别困难" value="特别困难" /><el-option label="困难" value="困难" /><el-option label="一般困难" value="一般困难" />
        </el-select>
      </el-form-item>
    </FormDialog>

    <!-- ============== 弹窗：组织结构新增/编辑 ============== -->
    <FormDialog v-model:visible="orgDialog" :title="orgTitle" :formData="orgForm" :rules="orgRules" :loading="orgSaving" @submit="handleOrgSave">
      <!-- 学院表单 -->
      <template v-if="orgType==='college'">
        <el-form-item label="学院编码" prop="collegeCode"><el-input v-model="orgForm.collegeCode" placeholder="如 CS" maxlength="32" /></el-form-item>
        <el-form-item label="学院名称" prop="collegeName"><el-input v-model="orgForm.collegeName" placeholder="如 计算机科学与技术学院" maxlength="64" /></el-form-item>
      </template>
      <!-- 专业表单 -->
      <template v-else-if="orgType==='major'">
        <el-form-item label="专业编码" prop="majorCode"><el-input v-model="orgForm.majorCode" placeholder="如 CS001" maxlength="32" /></el-form-item>
        <el-form-item label="专业名称" prop="majorName"><el-input v-model="orgForm.majorName" placeholder="如 计算机科学与技术" maxlength="64" /></el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select v-model="orgForm.collegeId" placeholder="请选择学院"><el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" /></el-select>
        </el-form-item>
      </template>
      <!-- 年级表单 -->
      <template v-else-if="orgType==='grade'">
        <el-form-item label="年级编码" prop="gradeCode"><el-input v-model="orgForm.gradeCode" placeholder="如 2026" maxlength="32" /></el-form-item>
        <el-form-item label="年级名称" prop="gradeName"><el-input v-model="orgForm.gradeName" placeholder="如 2026级" maxlength="32" /></el-form-item>
      </template>
      <!-- 班级表单 -->
      <template v-else-if="orgType==='class'">
        <el-form-item label="班级编码" prop="classCode"><el-input v-model="orgForm.classCode" placeholder="如 CS2601" maxlength="32" /></el-form-item>
        <el-form-item label="班级名称" prop="className"><el-input v-model="orgForm.className" placeholder="如 计科2601" maxlength="64" /></el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select v-model="orgForm.collegeId" placeholder="请选择学院"><el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" /></el-select>
        </el-form-item>
        <el-form-item label="所属专业" prop="majorId">
          <el-select v-model="orgForm.majorId" placeholder="请选择专业" :disabled="!orgForm.collegeId"><el-option v-for="m in filterMajors" :key="m.id" :label="m.majorName" :value="m.id" /></el-select>
        </el-form-item>
        <el-form-item label="所属年级" prop="gradeId">
          <el-select v-model="orgForm.gradeId" placeholder="请选择年级"><el-option v-for="g in grades" :key="g.id" :label="g.gradeName" :value="g.id" /></el-select>
        </el-form-item>
      </template>
    </FormDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import FormDialog from '../components/FormDialog.vue'
import { listUsersAPI, createUserAPI, updateUserAPI, toggleUserStatusAPI, collegeAPI, majorAPI, gradeAPI, classAPI, studentAPI } from '../api/index.js'

// ========== Tab 状态 ==========
const activeTab = ref('users')
const orgTab = ref('college')

// ========== 角色选项 ==========
const roleOptions = [{ id:1,roleName:'学生' },{ id:2,roleName:'辅导员' },{ id:3,roleName:'学院管理员' },{ id:4,roleName:'学校管理员' }]
const roleLabel = { STUDENT:'学生',COUNSELOR:'辅导员',COLLEGE:'学院管理员',SCHOOL:'学校管理员' }

// ==================== 用户管理 ====================
const users = ref([]); const userSearch = ref('')
const filteredUsers = computed(()=>{ const kw=userSearch.value.toLowerCase(); return kw?users.value.filter(u=>u.loginName.toLowerCase().includes(kw)):users.value })
async function loadUsers(){ const {data}=await listUsersAPI(); users.value=data.data }
const userDialog=ref(false); const userIsEdit=ref(false); const userEditId=ref(null); const userSaving=ref(false)
const userForm=reactive({ loginName:'',password:'',roleIds:[],remark:'' })
const userRules={ loginName:[{required:true,message:'请输入用户名',trigger:'blur'}],password:[{required:true,message:'请输入密码',trigger:'blur'}],roleIds:[{required:true,message:'请选择角色',trigger:'change'}] }
function openUserCreate(){ userIsEdit.value=false;userEditId.value=null;userForm.loginName='';userForm.password='';userForm.roleIds=[];userForm.remark='';userRules.password[0].required=true;userDialog.value=true }
function openUserEdit(row){ userIsEdit.value=true;userEditId.value=row.id;userForm.loginName=row.loginName;userForm.password='';userForm.roleIds=[...row.roleIds];userForm.remark=row.remark||'';userRules.password[0].required=false;userDialog.value=true }
async function handleUserSave(){ userSaving.value=true; try{ userIsEdit.value?await updateUserAPI(userEditId.value,{loginName:userForm.loginName,remark:userForm.remark,roleIds:userForm.roleIds}):await createUserAPI({loginName:userForm.loginName,password:userForm.password,remark:userForm.remark,roleIds:userForm.roleIds}); ElMessage.success(userIsEdit.value?'更新成功':'新增成功');userDialog.value=false;loadUsers() }catch(e){ ElMessage.error(e.response?.data?.message||'操作失败') }finally{ userSaving.value=false } }
async function handleUserToggle(row){ const act=row.deleted===0?'停用':'启用'; try{ await ElMessageBox.confirm(`确定${act}用户「${row.loginName}」？`,`${act}确认`,{confirmButtonText:act,cancelButtonText:'取消',type:'warning'}); await toggleUserStatusAPI(row.id); ElMessage.success(act+'成功');loadUsers() }catch{ } }

// ==================== 组织结构 ====================
const colleges=ref([]); const majors=ref([]); const grades=ref([]); const classes=ref([])
const majorFilter=ref(null); const classFilter=reactive({ collegeId:null,gradeId:null })
async function loadColleges(){ const {data}=await collegeAPI.list(); colleges.value=data.data }
async function loadMajors(){ const {data}=await majorAPI.list(majorFilter.value); majors.value=data.data }
async function loadGrades(){ const {data}=await gradeAPI.list(); grades.value=data.data }
async function loadClasses(){ const {data}=await classAPI.list({ collegeId:classFilter.collegeId||undefined, gradeId:classFilter.gradeId||undefined }); classes.value=data.data }
function getCollegeName(id){ return colleges.value.find(c=>c.id===id)?.collegeName||'' }
function getGradeName(id){ return grades.value.find(g=>g.id===id)?.gradeName||'' }
const filterMajors=computed(()=>orgForm.collegeId?majors.value.filter(m=>m.collegeId===orgForm.collegeId):majors.value)

// 弹窗
const orgDialog=ref(false); const orgType=ref('college'); const orgIsEdit=ref(false); const orgEditId=ref(null); const orgSaving=ref(false)
const orgForm=reactive({ collegeCode:'',collegeName:'',majorCode:'',majorName:'',collegeId:null,gradeCode:'',gradeName:'',classCode:'',className:'',majorId:null,gradeId:null })
const orgRules={ collegeCode:[{required:true,message:'请输入学院编码'}],collegeName:[{required:true,message:'请输入学院名称'}],majorCode:[{required:true,message:'请输入专业编码'}],majorName:[{required:true,message:'请输入专业名称'}],collegeId:[{required:true,message:'请选择学院'}],gradeCode:[{required:true,message:'请输入年级编码'}],gradeName:[{required:true,message:'请输入年级名称'}],classCode:[{required:true,message:'请输入班级编码'}],className:[{required:true,message:'请输入班级名称'}],majorId:[{required:true,message:'请选择专业'}],gradeId:[{required:true,message:'请选择年级'}] }
const orgTitle=computed(()=>{ const map={college:'学院',major:'专业',grade:'年级',class:'班级'}; return (orgIsEdit.value?'编辑':'新增')+map[orgType.value] })
function openOrgDialog(type,row){ orgType.value=type;orgIsEdit.value=!!row;orgEditId.value=row?.id; resetOrgForm(); if(row)Object.assign(orgForm,row); orgDialog.value=true }
function resetOrgForm(){ orgForm.collegeCode='';orgForm.collegeName='';orgForm.majorCode='';orgForm.majorName='';orgForm.collegeId=null;orgForm.gradeCode='';orgForm.gradeName='';orgForm.classCode='';orgForm.className='';orgForm.majorId=null;orgForm.gradeId=null }
async function handleOrgSave(){ orgSaving.value=true; try{ const api={college:collegeAPI,major:majorAPI,grade:gradeAPI,class:classAPI}[orgType.value]; const data=buildOrgData(); orgIsEdit.value?await api.update(orgEditId.value,data):await api.create(data); ElMessage.success(orgIsEdit.value?'更新成功':'新增成功');orgDialog.value=false; loadAllOrg() }catch(e){ ElMessage.error(e.response?.data?.message||'操作失败') }finally{ orgSaving.value=false } }
function buildOrgData(){ switch(orgType.value){ case'college':return {collegeCode:orgForm.collegeCode,collegeName:orgForm.collegeName}; case'major':return {majorCode:orgForm.majorCode,majorName:orgForm.majorName,collegeId:orgForm.collegeId}; case'grade':return {gradeCode:orgForm.gradeCode,gradeName:orgForm.gradeName}; case'class':return {classCode:orgForm.classCode,className:orgForm.className,collegeId:orgForm.collegeId,majorId:orgForm.majorId,gradeId:orgForm.gradeId}; default:return {} } }
async function handleOrgToggle(type,row){ const act=row.enabled?'停用':'启用'; const name=row.collegeName||row.majorName||row.gradeName||row.className; try{ await ElMessageBox.confirm(`确定${act}「${name}」？`,null,{confirmButtonText:act,cancelButtonText:'取消',type:'warning'}); const api={college:collegeAPI,major:majorAPI,grade:gradeAPI,class:classAPI}[type]; await api.toggle(row.id); ElMessage.success(act+'成功');loadAllOrg() }catch{ } }
function loadAllOrg(){ loadColleges();loadMajors();loadGrades();loadClasses() }

// ==================== 学生管理 ====================
const students=ref([])
const stuFilter=reactive({ studentNo:'',studentName:'',collegeId:null,majorId:null,gradeId:null,classId:null })
const stuMajors=ref([])
async function loadStudents(){ const p={};if(stuFilter.studentNo)p.studentNo=stuFilter.studentNo;if(stuFilter.studentName)p.studentName=stuFilter.studentName;if(stuFilter.collegeId)p.collegeId=stuFilter.collegeId;if(stuFilter.majorId)p.majorId=stuFilter.majorId;if(stuFilter.gradeId)p.gradeId=stuFilter.gradeId;if(stuFilter.classId)p.classId=stuFilter.classId; const {data}=await studentAPI.list(p);students.value=data.data }
async function onCollegeChange(){ stuFilter.majorId=null;if(stuFilter.collegeId){const {data}=await majorAPI.list(stuFilter.collegeId);stuMajors.value=data.data}else stuMajors.value=[];loadStudents() }
async function resetStuFilter(){ Object.assign(stuFilter,{studentNo:'',studentName:'',collegeId:null,majorId:null,gradeId:null,classId:null});stuMajors.value=[];loadStudents() }
function getMajorName(id){ return majors.value.find(m=>m.id===id)?.majorName||'' }
function getClassName(id){ return classes.value.find(c=>c.id===id)?.className||'' }
const fileInput = ref(null)
function triggerUpload() { fileInput.value?.click() }

function downloadTemplate() {
  studentAPI.template().then(res => {
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a'); a.href = url
    a.download = '学生导入模板.xlsx'; a.click()
    window.URL.revokeObjectURL(url)
  }).catch(() => ElMessage.error('模板下载失败'))
}

function handleUpload(e) {
  const file = e.target.files?.[0]; if (!file) return
  const formData = new FormData(); formData.append('file', file)
  ElMessage.info('正在导入...')
  studentAPI.import(formData).then(res => {
    const r = res.data.data
    ElMessage.success(`完成：${r.success}条成功，${r.skipped}条跳过`)
    if (r.errors?.length) {
      setTimeout(() => {
        ElMessageBox.alert(r.errors.slice(0,10).join('<br>'), '跳过详情', { dangerouslyUseHTMLString:true, confirmButtonText:'知道了' })
      }, 800)
    }
    loadStudents()
  }).catch(() => ElMessage.error('导入失败，请检查文件格式'))
  e.target.value = ''  // 清空以便重复选择同一文件
}

const stuDialog=ref(false);const stuIsEdit=ref(false);const stuEditId=ref(null);const stuSaving=ref(false)
const stuForm=reactive({ studentNo:'',studentName:'',collegeId:null,majorId:null,gradeId:null,classId:null,phone:'',originLoan:0,campusLoan:0,difficultyLevel:'' })
const stuRules={ studentNo:[{required:true,message:'请输入学号'}],studentName:[{required:true,message:'请输入姓名'}],collegeId:[{required:true,message:'请选择学院'}],majorId:[{required:true,message:'请选择专业'}],gradeId:[{required:true,message:'请选择年级'}],classId:[{required:true,message:'请选择班级'}] }
const stuFormMajors=computed(()=>stuForm.collegeId?majors.value.filter(m=>m.collegeId===stuForm.collegeId):majors.value)
const stuFormClasses=computed(()=>classes.value.filter(c=>(!stuForm.collegeId||c.collegeId===stuForm.collegeId)&&(!stuForm.gradeId||c.gradeId===stuForm.gradeId)))
function openStuDialog(row){ stuIsEdit.value=!!row;stuEditId.value=row?.id; if(row){ Object.assign(stuForm,row) }else{ Object.assign(stuForm,{studentNo:'',studentName:'',collegeId:null,majorId:null,gradeId:null,classId:null,phone:'',originLoan:0,campusLoan:0,difficultyLevel:''}) } stuDialog.value=true }
async function onStuCollegeChange(){ stuForm.majorId=null;stuForm.classId=null }
async function handleStuSave(){ stuSaving.value=true; try{ const data={studentNo:stuForm.studentNo,studentName:stuForm.studentName,collegeId:stuForm.collegeId,majorId:stuForm.majorId,gradeId:stuForm.gradeId,classId:stuForm.classId,phone:stuForm.phone,originLoan:stuForm.originLoan,campusLoan:stuForm.campusLoan,difficultyLevel:stuForm.difficultyLevel}; stuIsEdit.value?await studentAPI.update(stuEditId.value,data):await studentAPI.create(data); ElMessage.success(stuIsEdit.value?'更新成功':'新增成功');stuDialog.value=false;loadStudents() }catch(e){ ElMessage.error(e.response?.data?.message||'操作失败') }finally{ stuSaving.value=false } }
async function handleStuToggle(row){ const act=row.enabled?'停用':'启用'; try{ await ElMessageBox.confirm(`确定${act}学生「${row.studentName}(${row.studentNo})」？`,null,{confirmButtonText:act,cancelButtonText:'取消',type:'warning'}); await studentAPI.toggle(row.id); ElMessage.success(act+'成功');loadStudents() }catch{} }

onMounted(()=>{ loadUsers();loadAllOrg();loadStudents() })
</script>

<style scoped>
.base-data-page { max-width:1328px }
.page-title { font-size:20px;font-weight:600;line-height:28px;color:#1F2937;margin:0 0 16px }
.card { background:#FFFFFF;border:1px solid #E5E7EB;border-radius:4px;padding:20px }
:deep(.el-tabs__header){ margin-bottom:0 }
:deep(.el-tabs__nav-wrap::after){ height:1px;background:#E5E7EB }
:deep(.el-tabs__item.is-active){ color:#1677FF }
:deep(.el-tabs__active-bar){ background:#1677FF }
.org-tabs :deep(.el-tabs__header){ margin-bottom:0;padding-left:0 }
.toolbar { display:flex;justify-content:space-between;align-items:flex-start;margin:16px 0;flex-wrap:wrap;gap:8px }
.filter-row { display:flex;align-items:center;gap:8px;flex-wrap:wrap }
.filter-right { display:flex;align-items:center;gap:8px }
.search-input{ width:240px }
.filter-select{ width:200px }
.filter-input{ width:150px }
.toolbar-hint{ font-size:14px;color:#6B7280 }
.tag{ margin-right:4px }
.op-btns{ display:flex;gap:8px;white-space:nowrap }
.data-table{ width:100% }
:deep(.el-table__header th){ background:#F3F6FA;font-weight:600;font-size:14px;color:#374151;height:44px }
:deep(.el-table td){ height:44px;font-size:14px }
:deep(.el-input__wrapper),:deep(.el-select__wrapper){ height:32px;border-radius:4px }
</style>
