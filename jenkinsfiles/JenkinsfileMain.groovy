@Library('jenkins-pipeline-library@master')_

// ========== 修改点1：移除不必要的参数和检查 ==========
// 在多分支项目中，分支信息由 Jenkins 自动提供，不需要手动配置

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                choice(name: 'DEPLOY_ENV', choices: ['staging', 'pre-prod', 'prod'], description: '部署环境'),
                booleanParam(name: 'ROLLBACK', defaultValue: false, description: '是否回滚'),
                string(name: 'ROLLBACK_VERSION', defaultValue: '', description: '回滚版本号'),
                booleanParam(name: 'IS_RELEASE', defaultValue: false, description: '正式发布'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）')
        ])
])

// ========== 修改点2：修复空指针异常，添加空值检查 ==========
// 调用共享库，传递必要配置
mainPipeline([
        // 基础配置
        projectName: params.PROJECT_NAME,
        org: 'yakiv-liu',
        repo: 'demo-helloworld',
        agentLabel: 'docker-jnlp-slave',
        defaultEmail: params.EMAIL_RECIPIENTS,

        // 用户选择参数
        deployEnv: params.DEPLOY_ENV,
        rollback: params.ROLLBACK?.toBoolean() ?: false,  // 修复空指针
        rollbackVersion: params.ROLLBACK_VERSION ?: '',
        isRelease: params.IS_RELEASE?.toBoolean() ?: false,  // 修复空指针

        // 依赖检查配置 - 修复空指针异常
        skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK?.toBoolean() ?: true,  // 关键修复

        // 项目特定配置
        appPort: 8085,
        environmentHosts: [
                staging: [host: '192.168.233.8'],
                'pre-prod': [host: '192.168.233.9'],
                prod: [host: '192.168.233.10']
        ]
])