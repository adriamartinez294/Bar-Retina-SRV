mainClass=$1
action=$2 

echo "Setting MAVEN_OPTS to: $MAVEN_OPTS"
echo "Main Class: $mainClass"

if [[ "$action" == "build" ]]; then
    echo "Generating JAR file with all dependencies..."
    mvn clean package -Dmaven.test.skip=true
    echo "JAR generated in target directory."
    if [ -f target/server-package.jar ]; then
        echo "Successfully generated JAR: target/server-package.jar"
    else
        echo "Failed to generate JAR."
        exit 1
    fi
else
    # Execute mvn command with the profile and main class as arguments
    execArg="-PrunMain -Dexec.mainClass=$mainClass -Djavafx.platform=$javafx_platform"
    echo "Exec args: $execArg"

    # Execute mvn command
    mvn clean test-compile exec:java $execArg -X
fi