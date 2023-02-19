FROM centos
ENV TOMCAT_DOWNLOAD_LINK=https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.5/bin/apache-tomcat-10.1.5.tar.gz
RUN yum install java -y
RUN mkdir /opt/tomcat/
WORKDIR /opt/tomcat
ADD $TOMCAT_DOWNLOAD_LINK /opt/tomcat
RUN tar xvfz apache*.tar.gz
RUN mv apache-tomcat-9.0.54/* /opt/tomcat
EXPOSE 8080
CMD ["/opt/tomcat/bin/catalina.sh", "run"]