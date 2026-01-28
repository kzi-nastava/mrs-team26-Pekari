FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install -g npm@11.8.0 && npm ci
COPY . .
EXPOSE 4200
CMD ["npm","start","--","--host","0.0.0.0","--port","4200"]
