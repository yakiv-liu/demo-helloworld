@Library('jenkins-pipeline-library@master')_

// 完全移除复杂的 Generic Webhook Trigger 配置
// 我们将依赖 Jenkins 的标准 GitHub 触发器

// ========== 新增：添加 properties 配置，定义 GitHub 分支源 ==========
properties([
        pipelineTriggers([
                [
                        $class: 'GitHubPushTrigger',
                        adminlist: '',
                        allowMembersOfWhitelistedOrgsAsAdmin: true,
                        cron: '',
                        triggerOnEvents: []
                ]
        ]),
        [
                $class: 'BuildDiscarderProperty',
                strategy: [
                        $class: 'LogRotator',
                        artifactDaysToKeepStr: '',
                        artifactNumToKeepStr: '',
                        daysToKeepStr: '10',
                        numToKeepStr: '8'
                ]
        ],
        [
                $class: 'ParametersDefinitionProperty',
                parameterDefinitions: [
                        [
                                $class: 'StringParameterDefinition',
                                name: 'PROJECT_NAME',
                                defaultValue: 'demo-helloworld',
                                description: '项目名称'
                        ],
                        [
                                $class: 'StringParameterDefinition',
                                name: 'EMAIL_RECIPIENTS',
                                defaultValue: '251934304@qq.com',
                                description: '邮件接收人'
                        ],
                        [
                                $class: 'BooleanParameterDefinition',
                                name: 'SKIP_DEPENDENCY_CHECK',
                                defaultValue: true,
                                description: '跳过依赖检查以加速构建（默认跳过）'
                        ],
                        [
                                $class: 'ChoiceParameterDefinition',
                                name: 'SCAN_INTENSITY',
                                choices: 'fast\nstandard\ndeep',
                                description: '安全扫描强度'
                        ]
                ]
        ]
])

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    // ========== 移除 parameters 块，因为已经在 properties 中定义 ==========

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
                        echo " - ${cause.shortDescription ?: cause.toString()}"
                    }

                    // 更宽松的 PR 事件检查
                    if (env.CHANGE_ID) {
                        echo "✅ 确认：这是 PR #${env.CHANGE_ID} 事件，继续执行PR流水线"
                        echo "PR 源分支: ${env.CHANGE_BRANCH}"
                        echo "PR 目标分支: ${env.CHANGE_TARGET}"
                    } else if (env.GIT_BRANCH && env.GIT_BRANCH.contains('PR-')) {
                        echo "✅ 确认：基于分支名称检测到 PR 事件"
                        echo "GIT_BRANCH: ${env.GIT_BRANCH}"
                    } else {
                        echo "⚠️ 没有检测到标准的 PR 事件环境变量"
                        echo "将继续执行，但某些功能可能无法正常工作"
                        // 不中断构建，继续执行
                    }
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