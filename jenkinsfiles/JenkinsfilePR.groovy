@Library('jenkins-pipeline-library@master')_

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    parameters {
        string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称')
        string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人')
        booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）')
        choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
    }

    stages {
        stage('Check Build Type') {
            steps {
                script {
                    echo "=== 构建类型检测 ==="
                    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                    echo "GIT_BRANCH: ${env.GIT_BRANCH}"

                    // ========== 修改点1：根据 BRANCH_NAME 判断构建类型 ==========
                    // Multibranch Pipeline 中：
                    // - PR 构建：BRANCH_NAME 格式为 PR-{number}（如 PR-22）
                    // - 分支构建：BRANCH_NAME 为分支名（如 master、develop）

                    if (env.BRANCH_NAME && env.BRANCH_NAME.startsWith('PR-')) {
                        // 这是 PR 构建
                        def prNumber = env.BRANCH_NAME.replace('PR-', '')
                        echo "✅ 确认：这是 PR #${prNumber} 构建"
                        echo "构建类型：Pull Request 验证"
                    } else {
                        // 这是分支构建
                        echo "✅ 确认：这是分支构建"
                        echo "构建分支：${env.BRANCH_NAME}"
                        echo "构建类型：分支流水线"
                    }

                    // 打印构建原因
                    def causes = currentBuild.getBuildCauses()
                    echo "构建原因:"
                    causes.each { cause ->
                        echo " - ${cause.shortDescription ?: cause.toString()}"
                    }
                }
            }
        }

        stage('Run PR Pipeline') {
            steps {
                script {
                    // ========== 修改点2：根据构建类型传递不同参数 ==========
                    def config = [
                            projectName: params.PROJECT_NAME,
                            org: 'yakiv-liu',
                            repo: 'demo-helloworld',
                            agentLabel: 'docker-jnlp-slave',
                            defaultBranch: 'main',
                            defaultEmail: params.EMAIL_RECIPIENTS,
                            skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
                            scanIntensity: params.SCAN_INTENSITY
                    ]

                    // 如果是 PR 构建，提取 PR 编号
                    if (env.BRANCH_NAME && env.BRANCH_NAME.startsWith('PR-')) {
                        def prNumber = env.BRANCH_NAME.replace('PR-', '')
                        config.prNumber = prNumber
                        echo "🚀 执行 PR #${prNumber} 流水线"
                    } else {
                        echo "🚀 执行分支流水线"
                    }

                    prPipeline(config)
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline 执行完成 - 结果: ${currentBuild.result}"
        }
        success {
            echo "✅ Pipeline 执行成功"
        }
        failure {
            echo "❌ Pipeline 执行失败"
        }
        unstable {
            echo "⚠️ Pipeline 执行不稳定"
        }
    }
}