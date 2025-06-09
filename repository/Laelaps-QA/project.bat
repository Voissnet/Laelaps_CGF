IF [%1]==[/?] GOTO DO_NOTHING
%M2_HOME%\bin\mvn archetype:generate -DgroupId=net.redvoiss.sms ^
	-DartifactId=%1 ^
	-DarchetypeArtifactId=pom-root ^
	-DarchetypeGroupId=org.codehaus.mojo.archetypes ^
	-DinteractiveMode=false
:DO_NOTHING
echo Name is missing 
:DONE