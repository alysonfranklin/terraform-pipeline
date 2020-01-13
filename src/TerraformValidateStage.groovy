class TerraformValidateStage implements Stage {
    private static final DEFAULT_PLUGINS = [new TerraformPlugin()]
    private Jenkinsfile jenkinsfile
    private Map<String,Closure> decorations

    private static plugins = DEFAULT_PLUGINS.clone()

    public static final String ALL = 'all'
    public static final String VALIDATE = 'validate'

    public TerraformValidateStage() {
        this.jenkinsfile = Jenkinsfile.instance
        this.decorations = new HashMap<String,Closure>()
    }

    public Stage then(Stage nextStage) {
        return new BuildGraph(this).then(nextStage)
    }

    public void build() {
        Jenkinsfile.build(pipelineConfiguration())
    }

    private Closure pipelineConfiguration() {
        applyPlugins()

        def validateCommand = TerraformValidateCommand.instance()

        return {
            node {
                deleteDir()
                checkout(scm)

                applyDecorations(ALL) {
                    stage("validate") {
                        applyDecorations(VALIDATE) {
                            sh validateCommand.toString()
                        }
                    }
                }
            }
        }
    }

    private void applyDecorations(String stageName, Closure stageClosure) {
        def stageDecorations = decorations.get(stageName) ?: { stage -> stage() }
        stageDecorations.delegate = jenkinsfile
        stageDecorations(stageClosure)
    }

    public decorate(String stageName, Closure decoration) {
        def existingDecorations = decorations.get(stageName) ?: { stage -> stage() }

        def newDecoration = { stage ->
            decoration.delegate = delegate
            decoration.resolveStrategy = Closure.DELEGATE_FIRST
            decoration() {
                stage.delegate = delegate
                existingDecorations.delegate = delegate
                existingDecorations(stage)
            }
        }

        decorations.put(stageName,newDecoration)
    }

    private void applyDecorationsAround(String stageName, Closure stageClosure) {
        applyDecorations("Around-${stageName}", stageClosure)
    }

    public decorateAround(String stageName, Closure decoration) {
        decorate("Around-${stageName}", decoration)
    }

    public static addPlugin(plugin) {
        plugins << plugin
    }

    public void applyPlugins() {
        for(plugin in plugins) {
            plugin.apply(this)
        }
    }

    public static getPlugins() {
        return plugins
    }

    public static void resetPlugins() {
        this.plugins = DEFAULT_PLUGINS.clone()
    }
}
