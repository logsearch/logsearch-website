FROM cmfatih/nodejs

RUN apt-get install git -y
RUN npm install -g grunt-cli
RUN npm install -g bower
RUN wget http://go-cli.s3-website-us-east-1.amazonaws.com/releases/latest/cf-cli_amd64.deb && dpkg -i cf-cli_amd64.deb

VOLUME ["/workspace"]
WORKDIR /workspace