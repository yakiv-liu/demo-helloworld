@Library('jenkins-pipeline-library@master')_

def isPR = env.CHANGE_ID != null
print "change_id的值是：${env.CHANGE_ID}"
print "ispr的值: ${isPR}"
print "branch name is: ${env.BRANCH_NAME}"

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'PROJECT_REPO_URL', defaultValue: 'git@github.com:yakiv-liu/demo-helloworld.git', description: '项目代码仓库 URL'),
                string(name: 'PROJECT_BRANCH', defaultValue: 'master', description: '项目代码分支（默认：master）'),
                choice(name: 'BUILD_MODE', choices: ['full-pipeline', 'build-only'], description: '构建模式：build-only(仅构建) 或 full-pipeline(完整流水线)'),
                choice(name: 'DEPLOY_ENV', choices: ['staging', 'pre-prod', 'prod'], description: '部署环境'),
                booleanParam(name: 'ROLLBACK', defaultValue: false, description: '是否回滚'),
                string(name: 'ROLLBACK_VERSION', defaultValue: '', description: '回滚版本号'),
                // === 新增参数：控制是否跳过依赖检查 ===
                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
        ])
])

// 调用共享库，传递所有必要配置
mainPipeline([
        // 基础配置
        projectName: params.PROJECT_NAME,
        projectRepoUrl: params.PROJECT_REPO_URL,
        projectBranch: params.PROJECT_BRANCH,  // 传递分支配置
        org: 'yakiv-liu',
        repo: 'demo-helloworld',
        agentLabel: 'docker-jnlp-slave',
        defaultEmail: params.EMAIL_RECIPIENTS,

        // 用户选择参数
        deployEnv: params.DEPLOY_ENV,
        rollback: params.ROLLBACK.toBoolean(),
        rollbackVersion: params.ROLLBACK_VERSION,
//        isRelease: params.IS_RELEASE.toBoolean(),
        // === 新增配置：构建模式 ===
        buildMode: params.BUILD_MODE,

        // === 新增配置：传递跳过依赖检查参数 ===
        skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),

        // 项目特定配置
        appPort: 8085,
        environmentHosts: [
                staging: [host: '192.168.233.8'],
                'pre-prod': [host: '192.168.233.9'],
                prod: [host: '192.168.233.10']
        ]
])