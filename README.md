# YAML plugin

A poorly named YAML plugin for IntelliJ for team 449's 
robot maps. Doesn't actually replace the YAML plugin 
that IntelliJ comes with.

Features:
* Code completion (partially implemented)
* Checking if all required parameters are filled (kinda implemented)
* References using '@id' (implemented)
* Resolving references to Java classes; 
find usages for Java classes in YAML code (partially implemented)
* Type checking for parameters (not implemented)

How to install it:
1. First, clone this repository.
2. Go to File > Settings > Plugins
3. Click the gear icon and select 'Install plugin from disk'
4. Navigate to &lt;The folder this repository is in>\build\libs 
and select the jar labelled YamlPlugin-WHATEVERVERSION.jar