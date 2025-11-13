@Library('jenkins-pipeline-library@master')_

// 完全移除复杂的 Generic Webhook Trigger 配置
// 我们将依赖 Jenkins 的标准 GitHub 触发器

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    // ========== 修复：使用正确的 parameters 语法 ==========
    parameters {
        string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称')
        string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人')
        booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）')
        choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
    }

    stages {
        stage('Check PR Event') {
            steps {
                script {
                    echo "=== PR Pipeline 事件检测 ==="
                    echo "CHANGE_ID: ${env.CHANGE_ID}"
                    echo "CHANGE_BRANCH: ${env.CHANGE_BRANCH}"
                    echo "CHANGE_TARGET: ${env.CHANGE_TARGET}"
                    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                    echo "GIT_BRANCH: ${env.GIT_BRANCH}"

                    // 打印所有构建原因
                    def causes = currentBuild.getBuildCauses()
                    echo "构建原因:"
                    causes.each { cause ->
                        echo " - ${cause}"
                    }

                    // 检查是否是 PR 事件
                    if (!env.CHANGE_ID) {
                        echo "⚠️ 这不是 PR 事件，跳过 PR pipeline 执行"
                        echo "这可能是 PR merge 后的 push 事件，应该由 main pipeline 处理"
                        currentBuild.result = 'NOT_BUILT'
                        return
                    }

                    echo "✅ 确认：这是 PR #${env.CHANGE_ID} 事件，继续执行PR流水线"
                    echo "PR 源分支: ${env.CHANGE_BRANCH}"
                    echo "PR 目标分支: ${env.CHANGE_TARGET}"
                }
            }
        }

        stage('Run PR Pipeline') {
            steps {
                script {
                    // 调用共享库的PR流水线
                    prPipeline([
                            projectName: params.PROJECT_NAME,
                            org: 'yakiv-liu',
                            repo: 'demo-helloworld',
                            agentLabel: 'docker-jnlp-slave',
                            defaultBranch: 'main',
                            defaultEmail: params.EMAIL_RECIPIENTS,
                            skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
                            scanIntensity: params.SCAN_INTENSITY
                    ])
                }
            }
        }
    }
}