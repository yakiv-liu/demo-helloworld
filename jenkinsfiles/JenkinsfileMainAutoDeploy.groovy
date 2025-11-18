@Library('jenkins-pipeline-library@master')_

echo "✅ 确认：这是 main 分支的推送事件，继续执行main自动部署流水线"

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'PROJECT_REPO_URL', defaultValue: 'git@github.com:yakiv-liu/demo-helloworld.git', description: '项目代码仓库 URL'),
                string(name: 'PROJECT_BRANCH', defaultValue: 'main', description: '项目代码分支（默认：main）'),
                string(name: 'APP_PORT', defaultValue: '8085', description: '应用服务端口号'),
                string(name: 'AGENT_LABEL', defaultValue: 'docker-jnlp-slave', description: 'Jenkins Agent节点标签'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）')
        ])
])

// 调用共享库，传递所有必要配置
mainAutoDeployPipeline([
        // 基础配置
        projectName: params.PROJECT_NAME,
        projectRepoUrl: params.PROJECT_REPO_URL,
        projectBranch: params.PROJECT_BRANCH,
//        org: 'yakiv-liu',
//        repo: 'demo-helloworld',
        agentLabel: params.AGENT_LABEL,
        appPort: params.APP_PORT.toInteger(),
        defaultEmail: params.EMAIL_RECIPIENTS,
        // 跳过依赖检查参数
        skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
        // 项目特定配置
        environmentHosts: [
                staging: [host: '192.168.233.8'],
                'pre-prod': [host: '192.168.233.9'],
                prod: [host: '192.168.233.10']
        ]
])