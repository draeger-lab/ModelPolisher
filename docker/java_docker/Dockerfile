FROM openjdk:11-slim

# Set the maintainer email for the image
LABEL maintainer="zajac.thomas1992@gmail.com"

# Copy the ModelPolisher jar file into the root directory of the image
COPY ModelPolisher-2.1.jar /

# Create necessary directories for Java preferences and set permissions
# Reference: https://stackoverflow.com/a/23961144
RUN mkdir -p /.java/.systemPrefs && mkdir /.java/.userPrefs && chmod -R 777 /.java

# Set the container to run the ModelPolisher jar file as the default executable
ENTRYPOINT ["java", "-jar", "/ModelPolisher-2.1.jar"]

# Set the default command to display the help message of ModelPolisher
CMD ["--help"]
