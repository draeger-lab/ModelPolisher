FROM openjdk:17-slim

# Set the maintainer email for the image
LABEL maintainer="dario.eltzner@student.uni-tuebingen.de"

# Copy the ModelPolisher jar file into the root directory of the image
COPY ../target/ModelPolisher-2.1-STANDALONE.jar /

# Set the container to run the ModelPolisher jar file as the default executable
ENTRYPOINT ["java", "-jar", "/ModelPolisher-2.1-STANDALONE.jar"]

# Set the default command to display the help message of ModelPolisher
CMD ["--help"]
