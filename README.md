# 개인과제 Project
## 비디오 예약 & 대여 관리
![image](https://user-images.githubusercontent.com/82795806/123117533-c8831880-d47c-11eb-9bda-98d5a0424bee.png)

### Repositories

- https://github.com/joycomi/wavve

*전체 소스 받기*
```
git clone https://github.com/joycomi/wavve.git
```

### Table of contents

- [서비스 시나리오](#서비스-시나리오)
  - [기능적 요구사항](#기능적-요구사항)
  - [비기능적 요구사항](#비기능적-요구사항)
- [분석/설계](#분석설계)
  - [AS-IS 조직 (Horizontally-Aligned)](#AS-IS-조직-(Horizontally-Aligned))
  - [TO-BE 조직 (Vertically-Aligned)](#TO-BE-조직-(Vertically-Aligned))
  - [Event Storming 최종 결과](#Event-Storming-최종-결과)
  - [기능 요구사항 Coverage](#기능-요구사항-Coverage)
  - [헥사고날 아키텍처 다이어그램 도출](#헥사고날-아키텍처-다이어그램-도출)
  - [System Architecture](#System-Architecture)
- [구현](#구현)
  - [DDD(Domain Driven Design)의 적용](#DDD(Domain-Driven-Design)의-적용)
  - [Gateway 적용](#Gateway-적용)
  - [CQRS](#CQRS)
  - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
  - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
- [운영](#운영)
  - [Deploy/ Pipeline](#Deploy/Pipeline)
  - [Config Map](#Config-Map)
  - [Persistence Volume](#Persistence-Volume)
  - [Autoscale (HPA)](#Autoscale-(HPA))
  - [Circuit Breaker](#Circuit-Breaker)
  - [Zero-Downtime deploy (Readiness Probe)](#Zero-Downtime-deploy-(Readiness-Probe))
  - [Self-healing (Liveness Probe)](#Self-healing-(Liveness-Probe))


# 서비스 시나리오

## 기능적 요구사항

* 관리자는 비디오 정보를 등록한다.
* 고객은 비디오를 예약 할 수 있다.
* 비디오 예약은 결제가 완료 되어야 할 수 있다.
* 고객은 비디오 예약을 취소 할 수 있다.
* 예약 취소 시 자동 환불 되며, 환불 정보는 별도 저장 관리된다.
* 고객은 예약된 비디오를 대여, 반납 할 수 있다.
* 비디오 상태는 등록,이용 가능여부가 관리 된다.
* 고객은 비디오 예약정보를 조회 확인 할 수 있다. 
* 예약대여 서비스는 게이트웨이를 통해 고객과 통신한다.


## 비기능적 요구사항
* 트랜잭션
    * 비디오 예약은 결제가 완료 되어야 할 수 있다. (Sync 호출)
* 장애격리
    * 비디오 정보 등록 기능은 예약 기능이 수행 되지 않더라도 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
    * 예약대여 시스템이 과중 되면 예약을 잠시동안 받지 않고 잠시후에 하도록 유도한다. Circuit breaker, fallback
* 성능
    * 고객은 MyPage에서 비디오 예약정보 및 상태를 확인 할 수 있어야 한다. (CQRS)


# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)
![Horizontally-Aligned](https://user-images.githubusercontent.com/2360083/119254418-278d0d80-bbf1-11eb-83d1-494ba83aeaf1.png)

## TO-BE 조직 (Vertically-Aligned)
![image](https://user-images.githubusercontent.com/82795806/123185344-ad41f880-d4d0-11eb-9f2d-321bf3050b55.png)

## Event Storming 최종 결과
![image](https://user-images.githubusercontent.com/82795806/123125804-ce302c80-d483-11eb-831b-158a242aa465.png)
```
- Policy의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Res)
```

![image](https://user-images.githubusercontent.com/82795806/123370202-35e89380-d5ba-11eb-80e3-18b521e55b2d.png)


## 기능 요구사항 Coverage

![image](https://user-images.githubusercontent.com/82795806/123370239-47ca3680-d5ba-11eb-9992-fd8eb68d1fae.png)

![image](https://user-images.githubusercontent.com/82795806/123370273-587aac80-d5ba-11eb-94f4-0008e39975f5.png)

![image](https://user-images.githubusercontent.com/82795806/123370300-67615f00-d5ba-11eb-8536-291f6e71693b.png)

## 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/82795806/123126199-25360180-d484-11eb-8a50-bf462e509a20.png)


## System Architecture
![image](https://user-images.githubusercontent.com/82795806/123126300-3bdc5880-d484-11eb-87bd-22a8203a1782.png)


# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라,구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다
(각자의 포트넘버는 8081 ~ 8084, 8088 이다)
```shell
cd video
mvn spring-boot:run

cd rental
mvn spring-boot:run 

cd pay
mvn spring-boot:run 

cd mypage 
mvn spring-boot:run

cd gateway
mvn spring-boot:run 
```

## DDD(Domain-Driven-Design)의 적용
msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다.
Entity Pattern 과 Repository Pattern을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

video 서비스의 Video.java 구현

(<code>video\src\main\java\video\Video.java</code>)
```java
... 생략 ...

@Entity
@Table(name="Video_table")
public class Video {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer videoId;
    private String title;
    private String status;

    @PrePersist
    public void onPostPersist(){
        //비디오 정보 등록
        VideoInfoRegistered videoInfoRegistered = new VideoInfoRegistered();
        this.setStatus("Resistered");
        BeanUtils.copyProperties(this, videoInfoRegistered);
        videoInfoRegistered.publishAfterCommit();
    }

    //@PostUpdate
    @PreUpdate
    public void onPostUpdate(){
        System.out.println("\n\n##### listener Video-PostUpdate : " + this.getVideoId().toString() + ": "+this.getStatus().toString() + "\n\n");

        StatusModified statusModified = new StatusModified();
        BeanUtils.copyProperties(this, statusModified);
        
        // Video Status Manage
        if(this.getStatus().matches("CANCELLED") || this.getStatus().matches("RETURNED") )
            this.setStatus("AVAILABLE");
        else //BOOKED, RENTED
            this.setStatus("NotAVAILABLE");

        statusModified.publishAfterCommit();
    }

... 생략 ...
}
```

 video 서비스의 PolicyHandler.java 구현

(<code>video\src\main\java\video\PolicyHandler.java</code>)
```java
@Service
public class PolicyHandler{
    @Autowired VideoRepository videoRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_ModifyStatus(@Payload Paid paid){

        if(!paid.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + paid.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(paid.getVideoId());
        Video video = videoOptional.get();

        video.setStatus(paid.getStatus());
        videoRepository.save(video);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookingCancelled_ModifyStatus(@Payload BookingCancelled bookingCancelled){

        if(!bookingCancelled.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + bookingCancelled.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(bookingCancelled.getVideoId());
        Video video = videoOptional.get();

        video.setStatus(bookingCancelled.getStatus());

        videoRepository.save(video);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverVideoRented_ModifyStatus(@Payload VideoRented videoRented){

        if(!videoRented.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + videoRented.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(videoRented.getVideoId());
        Video video = videoOptional.get();
        
        video.setStatus(videoRented.getStatus());

        videoRepository.save(video);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverVideoReturned_ModifyStatus(@Payload VideoReturned videoReturned){

        if(!videoReturned.validate()) return;

        System.out.println("\n\n##### listener ModifyStatus(video) : " + videoReturned.toJson() + "\n\n");

        Optional<Video> videoOptional = videoRepository.findById(videoReturned.getVideoId());
        Video video = videoOptional.get();
        
        video.setStatus(videoReturned.getStatus());

        videoRepository.save(video);
            
    }
... 생략 ...
```

 video 서비스의 RentalRepository.java

(<code>video\src\main\java\video\VideoRepository.java</code>)

```java
package video;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="videos", path="videos")
public interface VideoRepository extends PagingAndSortingRepository<Video, Integer>{

}
```

DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.

## Gateway 적용
API GateWay를 통하여 마이크로 서비스들의 진입점을 통일할 수 있다. 
다음과 같이 GateWay를 적용하였다.

<code>gateway\src\main\resources\application.yml</code>
```yaml
server:
  port: 8088
---
spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: video
          uri: http://localhost:8081
          predicates:
            - Path=/videos/** 
        - id: rental
          uri: http://localhost:8082
          predicates:
            - Path=/rentals/** 
        - id: pay
          uri: http://localhost:8083
          predicates:
            - Path=/pays/**,/refunds/** 
        - id: mypage
          uri: http://localhost:8084
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true
---
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: video
          uri: http://video:8080
          predicates:
            - Path=/videos/** 
        - id: rental
          uri: http://rental:8080
          predicates:
            - Path=/rentals/** 
        - id: pay
          uri: http://pay:8080
          predicates:
            - Path=/pays/**,/refunds/** 
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true
server:
  port: 8080
```  
-- rental 서비스의 GateWay 적용
![image](https://user-images.githubusercontent.com/82795806/123214470-3887b200-d502-11eb-98f2-3aa8b4568a8f.png)

## CQRS
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현하였다.

본 프로젝트에서 View 역할은 mypage 서비스가 수행한다.

-- 예약대여(rental) 실행 후 myPage 화면
![image](https://user-images.githubusercontent.com/82795806/123214654-771d6c80-d502-11eb-9505-40749d86ca39.png)

## 폴리글랏 퍼시스턴스
mypage 서비스의 DB와 video/rental/pay 서비스의 DB를 다른 DB를 사용하여 MSA간 서로 다른 종류의 DB간에도 문제 없이 동작하여 다형성을 만족하는지 확인하였다.
(폴리글랏을 만족)

|서비스|DB|pom.xml|
| :--: | :--: | :--: |
|video| H2 |![image](https://user-images.githubusercontent.com/2360083/121104579-4f10e680-c83d-11eb-8cf3-002c3d7ff8dc.png)|
|rental| H2 |![image](https://user-images.githubusercontent.com/2360083/121104579-4f10e680-c83d-11eb-8cf3-002c3d7ff8dc.png)|
|pay/refund| H2 |![image](https://user-images.githubusercontent.com/2360083/121104579-4f10e680-c83d-11eb-8cf3-002c3d7ff8dc.png)|
|mypage| HSQL |![image](https://user-images.githubusercontent.com/2360083/120982836-1842be00-c7b4-11eb-91de-ab01170133fd.png)|


## 동기식 호출과 Fallback 처리
분석단계에서의 조건 중 하나로  예약대여 시 정상 결제가 되지 않으면 예약이 불가능한 조건을 예약대여(rental)->(pay) 간의 동기 호출을 통해 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

-- rental 서비스 내 external/PayService.java 구현

```java
...
@FeignClient(name="pay", url="${api.pay.url}")
public interface PayService {

    @RequestMapping(method= RequestMethod.POST, path="/pays")
    public void payment(@RequestBody Pay pay);

}
```

-- rental 서비스 내 Req/Resp

<code>rental\src\main\java\video\Rental.java</code>
```java
    @PostPersist
    public void onPostPersist() throws Exception{

//Pay서비스로 예약정보 전달
        video.external.Pay pay = new video.external.Pay();
        pay.setRentId(this.getRentId());
        pay.setPrice(this.getRentPrice());
        pay.setPayStatus(this.getPayStatus()); //OK, NotOK
        pay.setVideoId(this.getVideoId()); 
        pay.setVideoTitle(this.getVideoTitle());
        pay.setMemId(this.getMemId());
        
        RentalApplication.applicationContext.getBean(video.external.PayService.class)
            .payment(pay);

        }

    @PostUpdate
    @PreUpdate
    public void onPostUpdate(){
        System.out.println("\n\n##### listener PreUpdate(Rental) : " + this.getRentId().toString()+": "+this.getStatus().toString() + "\n\n");

        // 예약취소, 대여, 반납 처리 시 이벤트 발생
        if("CANCEL".equals(this.getStatus())){
            BookingCancelled bookingCancelled = new BookingCancelled();
            this.setStatus("CANCELLED");
            this.setPayStatus("Refunded");
            BeanUtils.copyProperties(this, bookingCancelled);
            bookingCancelled.publishAfterCommit();

        }else if("RENT".equals(this.getStatus())){
            VideoRented videoRented = new VideoRented();
            this.setStatus("RENTED");
            BeanUtils.copyProperties(this, videoRented);
            videoRented.publishAfterCommit();
        
        }else if("RETURN".equals(this.getStatus())){
            VideoReturned videoReturned = new VideoReturned();
            this.setStatus("RETURNED");
            BeanUtils.copyProperties(this, videoReturned);
            videoReturned.publishAfterCommit();
        }

    }
```
-- rental 서비스 내 Req/Resp

* 비디오 예약 시, 결제 OK/NotOK를 Pay서비스에서 체크하여 예약 처리 여부 결정
<code>pay\src\main\java\video\Pay.java</code>
```java
    @PrePersist
    public void onPostPersist() throws Exception{
        System.out.println("\n\n##### listener Pay-onPostPersist:paid "+ this.getPayStatus().toString() +" ####\n\n");

        if(this.getPayStatus().matches("OK")){
            Paid paid = new Paid();
            this.setStatus("BOOKED");
            BeanUtils.copyProperties(this, paid);
            paid.publishAfterCommit();
        }else{
            throw new Exception("Pay is Not OK Received!!");
        }
    }

    //@PostUpdate
    @PreUpdate
    public void onPostUpdate(){
        System.out.println("\n\n##### listener Pay-onPostUpdate:Refunded "+ this.getRentId().toString()+": "+ this.getPrice().toString() +" ####\n\n");

        Refunded refunded = new Refunded();
        BeanUtils.copyProperties(this, refunded);
        refunded.publishAfterCommit();
    }
```

### 동작 확인
---
-- 결제가 OK이면, 예약 처리(BOOKED)
```sh

-비디오 정보등록
http POST http:///EXTERNAL-IP:8080/videos title=AAA

-비디오 예약
http POST http://EXTERNAL-IP:8080/rentals videoId=1 videoTitle=AAA rentPrice=1000 payStatus=OK memId=Z

```

- rentals

![image](https://user-images.githubusercontent.com/82795806/123216933-03c92a00-d505-11eb-96cc-bbd99f92075a.png)

- videos

![image](https://user-images.githubusercontent.com/82795806/123371112-0dfa2f80-d5bc-11eb-9aa6-198d8b7c5a60.png)

-- 결제가 NotOK이면, 예약 불가 처리(Internal Server Error)
```sh

-비디오 정보등록
-비디오 정보등록
http POST http:///EXTERNAL-IP:8080/videos title=AAB

-비디오 예약
http POST http://EXTERNAL-IP:8080/rentals videoId=2 videoTitle=AAB rentPrice=1000 payStatus=NotOK memId=Z
```
![image](https://user-images.githubusercontent.com/82795806/123217040-1ba0ae00-d505-11eb-9a8f-d61281a1078e.png)

# 운영
## Kafka 설치
```sh
-- 버전 확인 (3.xx 버전인지 확인)
helm version

-- helm 의 설치관리자를 위한 시스템 사용자 생성
kubectl --namespace kube-system create sa tiller
kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller

-- kafka 설치
helm repo add incubator https://charts.helm.sh/incubator
helm repo update
kubectl create ns kafka
helm install my-kafka --namespace kafka incubator/kafka

* (kafka 설치 후) kafka 실행 결과 조회
kubectl get all -n kafka
``` 
![image](https://user-images.githubusercontent.com/82795806/123200552-ec317780-d4eb-11eb-9627-21388a708745.png)

## Httpie 설치
```sh
pip install --upgrade httpie
```
## siege 실행
```sh
kubectl run siege --image=apexacme/siege-nginx -n wavve
```

## Deploy/ Pipeline
각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 Azure를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 cloudbuild.yml 에 포함되었다.

- Git Hub에서 소스 가져오기

```
git clone https://github.com/joycomi/wavve.git
```

- Build 하기

```bash
cd ~/wavve/gateway
mvn package

cd ~/wavve/video
mvn package

cd ~/wavve/rental
mvn package

cd ~/wavve/pay
mvn package

cd ~/wavve/mypage
mvn package
```

- Docker Image Build & Push 후 deploy/service 생성(yml이용)

```sh
-- 기본 namespace 설정
kubectl config set-context --current --namespace=wavve

-- namespace 생성
kubectl create ns wavve

cd ~/wavve/gateway
az acr build --registry wavve --image wavve.azurecr.io/gateway:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ~/wavve/video
az acr build --registry wavve --image wavve.azurecr.io/video:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ~/wavve/rental
az acr build --registry wavve --image wavve.azurecr.io/rental:latest .

cd kubernetes
kubectl apply -f configmap.yml #configmap 추가
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ~/wavve/pay
az acr build --registry wavve --image wavve.azurecr.io/pay:latest .

cd kubernetes
kubectl apply -f pay-pvc.yml #pvc 추가
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ~/wavve/mypage
az acr build --registry wavve --image wavve.azurecr.io/mypage:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml
```

- <code>gateway/kubernetes/deployment.yml</code> 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: wavve
  labels:
    app: gateway
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gateway
  template:
    metadata:
      labels:
        app: gateway
    spec:
      containers:
        - name: gateway
          image: wavve.azurecr.io/gateway:latest
          ports:
            - containerPort: 8080
```	  

- <code>gateway/kubernetes/service.yaml</code> 파일 

```yml
apiVersion: v1
kind: Service
metadata:
  name: gateway
  namespace: wavve
  labels:
    app: gateway
spec:
  ports:
    - port: 8080
      targetPort: 8080
  type: LoadBalancer
  selector:
    app: gateway
```	  

- <code>rental/kubernetes/deployment.yml</code> 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rental
  namespace: wavve
  labels:
    app: rental
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rental
  template:
    metadata:
      labels:
        app: rental
    spec:
      containers:
        - name: rental
          image: wavve.azurecr.io/rental:latest
          ports:
            - containerPort: 8080
          env:
            - name: pay-url
              valueFrom:
                configMapKeyRef:
                  name: apiurl
                  key: url
...
```	  

- <code>wavve/rental/kubernetes/service.yaml</code> 파일 

```yml
apiVersion: v1
kind: Service
metadata:
  name: rental
  namespace: wavve
  labels:
    app: rental
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: rental
```	  

- deploy 완료

![image](https://user-images.githubusercontent.com/82795806/123218892-383de580-d507-11eb-8b55-c7bb1be361d3.png)

***

## Config Map

- 변경 가능성이 있는 설정을 ConfigMap을 사용하여 관리  
  - rental 서비스에서 바라보는 pay 서비스 url 일부분을 ConfigMap 사용하여 구현​  

- rental > <code>PayService.java</code>에 추가(rental/src/main/java/external/PayService.java)
```java
@FeignClient(name="pay", url="${api.pay.url}")
public interface PayService {

    @RequestMapping(method= RequestMethod.POST, path="/pays")
    public void payment(@RequestBody Pay pay);

}
```

- rental > <code>application.yml</code>에 추가 (rental/src/main/resources/application.yml)​  
```yml
api:
  pay:
    url: ${pay-url}
```

- rental > <code>deployment.yml</code>에 추가 (rental/kubernetes/deployment.yml)
```yml
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rental
  template:
    metadata:
      labels:
        app: rental
    spec:
      containers:
        - name: rental
          image: wavve.azurecr.io/rental:latest
          ports:
            - containerPort: 8080
          env: # configmap 맵핑 추가
            - name: pay-url
              valueFrom:
                configMapKeyRef:
                  name: apiurl
                  key: url
...
```

- configmap 생성 후 조회

```sh
cd ~/wavve/rental/kubernetes
kubectl apply -f configmap.yml -n wavve
kubectl get cm apiurl -n wavve
```
![image](https://user-images.githubusercontent.com/82795806/123197833-1896c500-d4e7-11eb-8e06-0be674f14a68.png)

- <code>configmap.yml</code> 파일 (rental\kubernetes\configmap.yml)

```yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: apiurl
  namespace: wavve
data:
  url: http://pay:8080
```

- configmap 삭제 후, 에러 확인  

```sh
kubectl delete configmap apiurl -n wavve

kubectl delete -f deployment.yml
kubectl apply -f deployment.yml
```
![image](https://user-images.githubusercontent.com/82795806/123299319-5d0c7a00-d554-11eb-9393-ad6167d1c270.png)



```sh
kubectl describe pod/rental-5ccc5f69cc-sn9ks -n wavve
```
![image](https://user-images.githubusercontent.com/82795806/123299172-35b5ad00-d554-11eb-9c88-2dd0319add70.png)



## Persistence Volume
----------
PVC 생성 파일

<code>pay-pvc.yml</code> (pay\kubernetes\pay-pvc.yml)
- AccessModes: **ReadWriteMany**
- storeageClass: **azurefile**
```yml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pay-disk
  namespace: wavve
spec:
  accessModes:
  - ReadWriteMany
  storageClassName: azurefile
  resources:
    requests:
      storage: 1Gi
```

- Containers 아래 Volumn Mount 추가

<code>deployment.yml</code> (pay\kubernetes\deployment.yml)
```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pay
  namespace: wavve
  labels:
    app: pay
spec:
  replicas: 1
  selector:
    matchLabels:
      app: pay
  template:
    metadata:
      labels:
        app: pay
    spec:
      containers:
        - name: pay
          image: wavve.azurecr.io/pay:latest
          ports:
            - containerPort: 8080
          ...
          volumeMounts: # pvc 설정 추가1 #
            - name: volume
              mountPath: "/mnt/azure"
          ...
      volumes: # pvc 설정 추가2 #
      - name: volume
        persistentVolumeClaim:
          claimName: pay-disk
```

<code>application.yml 설정 추가</code> (pay\src\main\resources\application.yml)

```yml

spring:
  profiles: docker
  cloud:
  ... # pvc 설정 추가#
logging:
  level:
    root: info
  file: /mnt/azure/logs/pay.log
log:
  refund:
    path: /mnt/azure/
    directory: logs
    file: refunded.log
```

-- 마운트 경로에 logging file 생성 확인

```sh
kubectl exec -it pod/pay-7df9779d8f-vk4q9 -n wavve -- /bin/sh
$ cd /mnt/azure/logs
$ tail -n 20 -f pay.log
```
![image](https://user-images.githubusercontent.com/82795806/123204703-9660cd80-d4f3-11eb-8682-0687962e31f9.png)

![image](https://user-images.githubusercontent.com/82795806/123204760-b09aab80-d4f3-11eb-89bd-2f1192be4b05.png)

마운트 경로에 예약취소에 따른 refunded(환불정보) log 생성 확인
```sh
kubectl exec -it pod/pay-7df9779d8f-vk4q9 -n wavve -- /bin/sh
$ cd /mnt/azure/logs
$ tail -n 20 -f refunded.log
```
![image](https://user-images.githubusercontent.com/82795806/123372114-f6bc4180-d5bd-11eb-8ce6-c70a2187668d.png)

pay서비스 PolicyHandler 구현 (pay\src\main\java\video\PolicyHandler.java)

-- 예약 취소 시 환불 내역 log파일 저장

```java
@Service
public class PolicyHandler{
    @Autowired PayRepository payRepository;
    @Autowired RefundRepository refundRepository;

    @Value("${log.refund.path}")
    String PATH;

    @Value("${log.refund.directory}")
    String directoryName;

    @Value("${log.refund.file}")
    String file;
    ...
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRefunded_RegRefund(@Payload Refunded refunded) throws IOException {
        //환불내역 저장
        if(!refunded.validate()) return;

        Refund refund = new Refund();
        refund.setPayId(refunded.getPayId());
        refund.setPrice(refunded.getPrice());
        refund.setPayStatus("Refunded");
        refund.setRentId(refunded.getRentId());
        refundRepository.save(refund);

        //환불내역 log 파일 기록
        String str = refunded.toJson()+"\n";
        String fileName = PATH+directoryName+"/"+file;
        
        File file  = new File(String.valueOf(fileName));
        File directory = new File(String.valueOf(directoryName));
        
        if(!directory.exists()){
            directory.mkdir();
            if(!file.exists()){
                file.getParentFile().mkdir();
                file.createNewFile();
            }
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(str);
        bw.close();
    }
    ...
```

## Circuit Breaker

  * 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Istio를 설치하여, wavve namespace에 주입하여 구현함

시나리오는 예약(booking)-->백신(vaccine) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리.

- Istio 다운로드 및 PATH 추가, 설치, namespace에 istio주입

```sh
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.7.1 TARGET_ARCH=x86_64 sh -
※ istio v1.7.1은 Kubernetes 1.16이상에서만 동작
```

- istio PATH 추가

```sh
cd istio-1.7.1
export PATH=$PWD/bin:$PATH
```

- istio 설치 및 결과

```sh
istioctl install --set profile=demo --set hub=gcr.io/istio-release
※ Docker Hub Rate Limiting 우회 설정
```

![image](https://user-images.githubusercontent.com/82795806/123373337-33893800-d5c0-11eb-90dd-9bd41b9c5e75.png)

![image](https://user-images.githubusercontent.com/82795806/123373304-23715880-d5c0-11eb-9449-c8a98e0ea90c.png)

- namespace에 istio주입

```sh
$ kubectl label namespace wavve istio-injection=enabled
```

- Virsual Service 생성 (Timeout 3초 설정)
- wavve/rental/kubernetes/rental-istio.yaml 파일 

```yml
  apiVersion: networking.istio.io/v1alpha3
  kind: VirtualService
  metadata:
    name: vs-rental-network-rule
    namespace: wavve
  spec:
    hosts:
    - rental
    http:
    - route:
      - destination:
          host: rental
      timeout: 3s
```	  
![image](https://user-images.githubusercontent.com/82795806/123223846-34f92880-d50c-11eb-9769-e69f55ca94b4.png)


- 서비스(deploy) 재배포 후 Pod에 CB 부착 확인

![image](https://user-images.githubusercontent.com/82795806/123373188-edcc6f80-d5bf-11eb-9348-604725d78782.png)


- Siege pod에 진입하여 워크로드를 걸어준다.
```sh
kubectl exec -it pod/siege -c siege -n wavve -- /bin/bash
```

- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인

  (동시사용자 150명, 10초 동안 실시)

```sh
$ siege -c150 -t10S -v --content-type "application/json" 'http://rental:8080/rentals POST {"videoId":1, "videoTitle":"AAB", "rentPrice":1000, "payStatus":"OK", "memId":"Z"}'
```
![image](https://user-images.githubusercontent.com/82795806/123239397-46493180-d51a-11eb-9cf1-c9e7d4451191.png)


- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 
- 약 97%정도 정상적으로 처리되었음.

***

## Autoscale (HPA)

  앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- 예약대여 서비스에 리소스에 사용량 제한 설정을 추가한다.

<code>rental/kubernetes/deployment.yml</code>

```yml
  resources:
    requests:
      memory: "64Mi"
      cpu: "250m"
    limits:
      memory: "500Mi"
      cpu: "500m"
```

- rental vs를 삭제한다.

![image](https://user-images.githubusercontent.com/82795806/123242785-57477200-d51d-11eb-8943-d6f781c3d11d.png)


- 예약 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 10프로를 넘어서면 replica 를 10개까지 늘려준다:

```sh
$ kubectl autoscale deploy rental --min=1 --max=10 --cpu-percent=10
```
![image](https://user-images.githubusercontent.com/82795806/123242697-4139b180-d51d-11eb-9769-42b28fec2a03.png)

- Siege pod에 진입하여 워크로드를 걸어준다.
  
```sh
$ kubectl exec -it pod/siege -c siege -n wavve -- /bin/bash
```

- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인
  
  (동시사용자 150명, 10초 동안 실시)

```sh
$ siege -c150 -t10S -v --content-type "application/json" 'http://rental:8080/rentals POST {"videoId":1, "videoTitle":"AAB", "rentPrice":1000, "payStatus":"OK", "memId":"Z"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:

```sh
$ watch kubectl get all
```

- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

* siege 부하테스트 - 전

![image](https://user-images.githubusercontent.com/82795806/123238029-21a08a00-d519-11eb-8e63-2a8d8267bac8.png)

* siege 부하테스트 - 후

![image](https://user-images.githubusercontent.com/82795806/123241580-3f232300-d51c-11eb-8820-463143857081.png)


- siege 의 로그를 통해 전체적인 성공률이 높아진 것을 확인 할 수 있다.

![image](https://user-images.githubusercontent.com/82795806/123243196-bf965380-d51d-11eb-9e42-362672a8e8f4.png)

## Zero-Downtime deploy (Readiness Probe)

- video 서비스 deployment.yml에 정상 적용되어 있는 readinessProbe

```yml
readinessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 10
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 10
```

- deployment.yml에서 readiness 설정 제거 후, 배포중 siege 테스트 진행  
    - hpa 설정에 의해 target 지수 초과하여 rental scale-out 진행됨 

-- hpa 설정
```sh
$ kubectl autoscale deploy video --min=1 --max=10 --cpu-percent=10 -n wavve
```
![image](https://user-images.githubusercontent.com/82795806/123293855-49aae000-d54f-11eb-9ce8-bc775590d476.png)

-- video 서비스 초과 생성
![image](https://user-images.githubusercontent.com/82795806/123293757-37c93d00-d54f-11eb-864c-c30a79315dc5.png)

![image](https://user-images.githubusercontent.com/82795806/123294106-82e35000-d54f-11eb-8a1d-25fbb011fe6d.png)

-- video가 배포되는 중,  
정상 실행중인 video로의 요청은 성공(201),  
배포중인 booking으로의 요청은 실패(503 - Service Unavailable) 확인

![image](https://user-images.githubusercontent.com/82795806/123294468-d786cb00-d54f-11eb-85d2-9444550417c5.png)

- 다시 readiness 정상 적용 후, Availability 100% 확인  

![image](https://user-images.githubusercontent.com/82795806/123294686-0a30c380-d550-11eb-9339-b07a811b7b37.png)
    
## Self-healing (Liveness Probe)

- video deployment.yml에 정상 적용되어 있는 livenessProbe  
 
 
<code>video/kubernetes/deployment.yml</code>
```yml
livenessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 120
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 5
```

- port 및 path 잘못된 값으로 변경 된 yml 서비스 생성 후 retry 시도 확인

<code>video/kubernetes/failed_liveness.yml</code>
```yml
livenessProbe:
  httpGet:
    path: '/actuator/failed'
    port: 8090
  initialDelaySeconds: 120
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 5
```

- retry 시도 확인

![image](https://user-images.githubusercontent.com/82795806/123296427-9a233d00-d551-11eb-9aac-5b5b520d5322.png)

끝.