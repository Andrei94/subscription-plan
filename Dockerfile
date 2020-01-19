FROM oracle/graalvm-ce:19.3.1
RUN gu install native-image
ADD . /subscription-plan
WORKDIR /subscription-plan
CMD ./gradlew assemble && native-image --no-server --static -cp build/libs/subscription-plan-0.1-all.jar