@Library('jenkins-pipeline-library@master')_

// ========== 极简版本：只需调用共享库 ==========
prPipeline(
        projectName: 'demo-helloworld',
        org: 'yakiv-liu',
        repo: 'demo-helloworld',
        agentLabel: 'docker-jnlp-slave',
        defaultBranch: 'main',
        defaultEmail: '251934304@qq.com',
        skipDependencyCheck: true,
        scanIntensity: 'standard'
)