@Library('jenkins-pipeline-library@master')_

// ========== 新增：在 pipeline 外部进行目标分支检查 ==========
def allowedTargetBranches = ['master', 'main']
def shouldRunPipeline = (env.CHANGE_TARGET && allowedTargetBranches.contains(env.CHANGE_TARGET))


if (!shouldRunPipeline) {
    echo "⏭️ 跳过 PR - 目标分支 ${env.CHANGE_TARGET} 不在允许列表中"
    currentBuild.result = 'SUCCESS'
    currentBuild.description = "跳过 - 目标分支 ${env.CHANGE_TARGET} 不受支持"
    // 直接返回，不执行 pipeline
    return
}

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    parameters {
        string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称')
        string(name: 'APP_PORT', defaultValue: '8085', description: '应用服务端口号')
        string(name: 'AGENT_LABEL', defaultValue: 'docker-jnlp-slave', description: 'Jenkins Agent节点标签')
        string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人')
        booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）')
        choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
    }

    stages {
        stage('PR Info') {
            steps {
                script {
                    echo "✅ PR 目标分支验证通过: ${env.CHANGE_TARGET}"
                    echo "当前分支: ${env.BRANCH_NAME}"
                    echo "PR 源分支: ${env.CHANGE_BRANCH}"
                    echo "PR 编号: ${env.CHANGE_ID}"
                    currentBuild.description = "PR to ${env.CHANGE_TARGET}"
                }
            }
        }
        stage('Run PR Pipeline') {
            steps {
                script {
                    prPipeline([
                            projectName: params.PROJECT_NAME,
                            org: 'yakiv-liu',
                            repo: 'demo-helloworld',
                            agentLabel: params.AGENT_LABEL,
                            appPort: params.APP_PORT.toInteger(),
                            defaultEmail: params.EMAIL_RECIPIENTS,
                            skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
                            scanIntensity: params.SCAN_INTENSITY
                    ])
                }
            }
        }
    }

    post {
        always {
            script {
                echo "PR Pipeline 执行完成 - 结果: ${currentBuild.result}"
                // 清理工作空间
                cleanWs()
            }
        }
    }
}