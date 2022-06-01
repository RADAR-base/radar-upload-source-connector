#alpine version close to the version mentioned in the readme
FROM --platform=$BUILDPLATFORM node:14 as builder

RUN mkdir /app
WORKDIR /app

COPY package*.json /app/
RUN npm install

COPY . /app/

RUN npm run build --prod

FROM nginx:1.22

# add init script
COPY ./docker/nginx.conf /etc/nginx/nginx.conf

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist/ .

COPY ./docker/entrypoint.sh /entrypoint.sh

# expose internal port:80 and run init.sh
EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
