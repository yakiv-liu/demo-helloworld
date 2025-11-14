@Library('jenkins-pipeline-library@master')_

def isPR = env.CHANGE_ID != null
def isMainBranchPush = env.BRANCH_NAME == 'main' && !isPR

// 如果是 PR 事件，立即拒绝并给出明确提示
if (isPR) {
    currentBuild.displayName = "REJECTED-PR-${env.CHANGE_ID}"
    currentBuild.description = "PR事件应由PR流水线处理"
    error """🚫 PR事件路由错误！
    
            当前PR #${env.CHANGE_ID} 错误触发了 main-auto-deploy-pipeline。
            这应该由 pr-pipeline 处理。
            
            请检查：
            1. GitHub Webhook 配置
            2. Jenkins trigger 配置
            3. 确保 pr-pipeline 的 triggerForPr 设置为 true
            
            PR详细信息：
            - 源分支: ${env.CHANGE_BRANCH}
            - 目标分支: ${env.CHANGE_TARGET}
            - PR ID: ${env.CHANGE_ID}
    """
}

// 如果不是 main 分支的推送，也拒绝
if (!isMainBranchPush) {
    error "🚫 main-auto-deploy-pipeline 仅处理 main 分支的推送事件。当前分支: ${env.BRANCH_NAME}"
}

echo "✅ 确认：这是 main 分支的推送事件，继续执行main自动部署流水线"

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'PROJECT_REPO_URL', defaultValue: 'git@github.com:yakiv-liu/demo-helloworld.git', description: '项目代码仓库 URL'),
                string(name: 'PROJECT_BRANCH', defaultValue: 'main', description: '项目代码分支（默认：main）'),
                booleanParam(name: 'IS_RELEASE', defaultValue: false, description: '正式发布'),
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
        org: 'yakiv-liu',
        repo: 'demo-helloworld',
        agentLabel: 'docker-jnlp-slave',
        defaultEmail: params.EMAIL_RECIPIENTS,

        // 用户选择参数
        isRelease: params.IS_RELEASE.toBoolean(),

        // 跳过依赖检查参数
        skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),

        // 项目特定配置
        appPort: 8085,
        environmentHosts: [
                staging: [host: '192.168.233.8'],
                'pre-prod': [host: '192.168.233.9'],
                prod: [host: '192.168.233.10']
        ]
])