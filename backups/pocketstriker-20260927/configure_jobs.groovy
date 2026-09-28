import hudson.model.*
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.pipeline.modeldefinition.parser.Converter

['ios', 'assets'].each { kind ->
    assert Converter.scriptToPipelineDef(new File('/Users/daisei/MCombat_tool/pipeline_script/PocketStriker/' + kind + '.groovy').text) != null
}
['CustomIOSBuild_V': 'ios', 'AssetDev_V': 'assets'].each { name, kind ->
    def job = Jenkins.get().getItemByFullName(name)
    assert job != null && !job.isBuilding() && !job.isInQueue()
    def previous = job.definition
    assert previous instanceof CpsScmFlowDefinition
    assert previous.scm.userRemoteConfigs[0].url == 'https://github.com/bodacheng/MCombat_tool.git'
    def defs = job.getProperty(ParametersDefinitionProperty).parameterDefinitions.findAll { !(it.name in ['VALIDATE_ONLY', 'SIGNING_VALIDATE_ONLY']) && !(name == 'AssetDev_V' && it.name == 'CUSTOM_WORKSPACE') }.collect { p ->
        if (p.name == 'UNITY_VERSION') {
            return new ChoiceParameterDefinition('UNITY_VERSION', ['6000.5.1f1'] as String[], 'Unity 6.5 (6000.5.1f1)')
        }
        if (name == 'AssetDev_V' && p.name == 'ANDROID') {
            return new BooleanParameterDefinition('ANDROID', false, 'Optional Android resource build; default off.')
        }
        return p
    }
    defs.add(new BooleanParameterDefinition('VALIDATE_ONLY', false, 'Validate builds without S3/App Store upload; iOS native validation also skips signing and IPA export.'))
    if (name == 'CustomIOSBuild_V') {
        defs.add(new BooleanParameterDefinition('SIGNING_VALIDATE_ONLY', false, 'Run signed archive and IPA export, but skip App Store validation and upload.'))
    }
    job.removeProperty(ParametersDefinitionProperty)
    job.addProperty(new ParametersDefinitionProperty(defs))
    def definition = new CpsScmFlowDefinition(previous.scm, 'pipeline_script/PocketStriker/' + kind + '.groovy')
    definition.setLightweight(true)
    job.setDefinition(definition)
    job.save()
    println(name + ': Unity=' + job.getProperty(ParametersDefinitionProperty).getParameterDefinition('UNITY_VERSION').choices + ', pipeline=' + job.definition.scriptPath + ', VALIDATE_ONLY available')
}
println('Configuration updated for the two requested jobs only.')
