# 개인과제(문혜영) Project
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
* 고객은 예약된 비디오를 대여, 반납 할 수 있다.
* 비디오의 각 상태(등록,예약,예약취소, 대여, 반납)는 관리 된다.
* 고객은 비디오 예약정보를 조회 확인 할 수 있다. 
* 예약 서비스는 게이트웨이를 통해 고객과 통신한다.


## 비기능적 요구사항
* 트랜잭션
    * 비디오 예약은 결제가 완료 되어야 할 수 있다. (Sync 호출)
* 장애격리
    * 비디오 정보 등록 기능은 예약 기능이 수행 되지 않더라도 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
    * 예약시스템이 과중 되면 예약을 잠시동안 받지 않고 잠시후에 하도록 유도한다. Circuit breaker, fallback
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

![image](https://user-images.githubusercontent.com/82795806/123125987-fa4bad80-d483-11eb-8b13-57de2caa42d1.png)


## 기능 요구사항 Coverage

![image](https://user-images.githubusercontent.com/82795806/123188689-6c011700-d4d7-11eb-8bb4-db081970bc32.png)

![image](https://user-images.githubusercontent.com/82795806/123188812-a36fc380-d4d7-11eb-81d5-a7770b97b9fc.png)

![image](https://user-images.githubusercontent.com/82795806/123188843-b97d8400-d4d7-11eb-888f-b2f2e08c5f1c.png)

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
## DDD(Domain-Driven-Design)의 적용 (이하작성필요)
msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다.
Entity Pattern 과 Repository Pattern을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

rental 서비스의 rental.java 구현

```java

...
package video;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Rental_table")
public class Rental {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer rentId;
    private Integer videoId;
    private String videoTitle;
    private Integer rentPrice;
    private String status;
    private String memId;

    @PostPersist
    public void onPostPersist(){
        //예약&결제정보 전달
        VideoBooked videoBooked = new VideoBooked();
        BeanUtils.copyProperties(this, videoBooked);
        videoBooked.publishAfterCommit();

        //Pay서비스로 예약정보 전달
        video.external.Pay pay = new video.external.Pay();
        pay.setRentId(this.getRentId());
        pay.setPrice(this.getRentPrice());
        pay.setPayStatus(this.getStatus()); //OK, NotOK
        pay.setVideoId(this.getVideoId());

        // mappings goes here
         RentalApplication.applicationContext.getBean(video.external.PayService.class)
            .payment(pay);

        }

    @PostUpdate
    public void onPostUpdate(){

        // 예약취소, 대여, 반납 처리 시 이벤트 발생
        if("CANCEL".equals(this.getStatus())){
            BookingCancelled bookingCancelled = new BookingCancelled();
            this.setStatus("CANCELLED");
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

... 이하 생략 ...
}
```

 rental 서비스의 PolicyHandler.java 구현

```java
package video;

import video.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyHandler{
    @Autowired RentalRepository rentalRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverStatusModified_ModifyStatus(@Payload StatusModified statusModified){

        if(!statusModified.validate()) return;

        Iterable<Rental> rentals= rentalRepository.findAll();
        
        for (Rental rental : rentals) {
            if(rental.getVideoId().equals(statusModified.getVideoId()))
            {
                rental.setStatus(statusModified.getStatus());
                rentalRepository.save(rental);

                break;
            }
        }
           
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}

}
```

 rental 서비스의 BookingRepository.java


```java
package video;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="rentals", path="rentals")
public interface RentalRepository extends PagingAndSortingRepository<Rental, Integer>{

}
```

DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.

## Gateway 적용
API GateWay를 통하여 마이크로 서비스들의 진입점을 통일할 수 있다. 
다음과 같이 GateWay를 적용하였다.

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
mypage 서비스의 GateWay 적용

![image](https://user-images.githubusercontent.com/82795806/123207410-4c2e1b00-d4f8-11eb-9a3b-e4d00cc2bc3d.png)


## CQRS
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현하였다

본 프로젝트에서 View 역할은 mypage 서비스가 수행한다.

예약(Booked) 실행 후 myPage 화면
 
![image](https://user-images.githubusercontent.com/82795860/121005958-526b8a00-c7cb-11eb-9bae-ad4bd70ef2eb.png)



![image](https://user-images.githubusercontent.com/82795860/121006311-bb530200-c7cb-11eb-9d85-a7b22d1a2729.png)
  
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
분석단계에서의 조건 중 하나로  접종 예약 수량은 백신 재고수량을 초과 할 수 없으며
예약(Booking)->(Vaccine) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.



Booking 서비스 내 external.VaccineService

```java
package anticorona.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@FeignClient(name="vaccine", url="http://${api.url.vaccine}:8080")
public interface VaccineService {

    @RequestMapping(method= RequestMethod.GET, path="/vaccines/checkAndBookStock")
    public boolean checkAndBookStock(@RequestParam Long vaccineId);

}
```

Booking 서비스 내 Req/Resp

```java
    @PostPersist
    public void onPostPersist() throws Exception {
        if(BookingApplication.applicationContext.getBean(anticorona.external.VaccineService.class)
            .checkAndBookStock(this.vaccineId)){
                Booked booked = new Booked();
                BeanUtils.copyProperties(this, booked);
                booked.publishAfterCommit();
            }
        else{
            throw new Exception("Out of Stock Exception Raised.");
        }

    }
```

Vaccine 서비스 내 Booking 서비스 Feign Client 요청 대상

```java
 @RestController
 public class VaccineController {

     @Autowired
     VaccineRepository vaccineRepository;

     @RequestMapping(value = "/vaccines/checkAndBookStock",
        method = RequestMethod.GET,
        produces = "application/json;charset=UTF-8")
    public boolean checkAndBookStock(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("##### /vaccine/checkAndBookStock  called #####");

        boolean status = false;

        Long vaccineId = Long.valueOf(request.getParameter("vaccineId"));
        
        Optional<Vaccine> vaccine = vaccineRepository.findById(vaccineId);
        if(vaccine.isPresent()){
            Vaccine vaccineValue = vaccine.get();
            //예약 가능한지 체크 
            if(vaccineValue.getStock() - vaccineValue.getBookQty() > 0) {
                //예약 가능하면 예약수량 증가
                status = true;
                vaccineValue.setBookQty(vaccineValue.getBookQty() + 1);
                vaccineRepository.save(vaccineValue);
            }
        }

        return status;
     }
 }

```

동작 확인

접종 예약하기 시도 시  백신의 재고 수량을 체크함

![image](https://user-images.githubusercontent.com/82795860/120994076-1e8a6780-c7bf-11eb-8374-53f7a4336a1a.png)


접종 예약 시 백신 재고수량을 초과하지 않으면 예약 가능

![image](https://user-images.githubusercontent.com/82795860/120997798-78406100-c7c2-11eb-90fa-b8ff71f53c77.png)


접종 예약시 백신 재고수량을 초과하여 예약시 예약안됨

![image](https://user-images.githubusercontent.com/82795860/120993294-5b099380-c7be-11eb-8970-b2b0e28d6e40.png)

  
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

# Httpie 설치
pip install --upgrade httpie


## Deploy/ Pipeline
각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 Azure를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 cloudbuild.yml 에 포함되었다.


- git에서 소스 가져오기

```
git clone --recurse-submodules https://github.com/dt-3team/anticorona.git
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

- Docker Image Push/deploy/서비스생성(yml이용)

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

- wavve/gateway/kubernetes/deployment.yml 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: anticorona
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
          image: skccanticorona.azurecr.io/gateway:latest
          ports:
            - containerPort: 8080
```	  

- anticorona/gateway/kubernetes/service.yaml 파일 

```yml
apiVersion: v1
kind: Service
metadata:
  name: gateway
  namespace: anticorona
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

- anticorona/booking/kubernetes/deployment.yml 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking
  namespace: anticorona
  labels:
    app: booking
spec:
  replicas: 1
  selector:
    matchLabels:
      app: booking
  template:
    metadata:
      labels:
        app: booking
    spec:
      containers:
        - name: booking
          image: skccanticorona.azurecr.io/booking:latest
          ports:
            - containerPort: 8080
          env:
            - name: vaccine-url
              valueFrom:
                configMapKeyRef:
                  name: apiurl
                  key: url
```	  

- anticorona/booking/kubernetes/service.yaml 파일 

```yml
apiVersion: v1
kind: Service
metadata:
  name: booking
  namespace: anticorona
  labels:
    app: booking
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: booking
```	  

- deploy 완료

![image](https://user-images.githubusercontent.com/82795806/123196422-da000b00-d4e4-11eb-818d-be66dffac811.png)

![image](https://user-images.githubusercontent.com/82795806/120998532-24824780-c7c3-11eb-8f01-d73860d68426.png)

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
![image](https://user-images.githubusercontent.com/82795806/123198552-44ff1100-d4e8-11eb-93e5-c1ef0695fdb6.png)


```sh
kubectl describe pod/rental-5ccc5f69cc-sn9ks -n wavve
```
![image](https://user-images.githubusercontent.com/82795806/123198687-85f72580-d4e8-11eb-94c6-82e76bda6b9c.png)


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

<code>deployment.yml</code> (pay\kubernetes\deployment.yml)

- Container에 Volumn Mount

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

<code>application.yml</code> (pay\src\main\resources\application.yml)
- profile: **docker**
- logging.file: PVC Mount 경로
<code>application.yml</code>
```yml

spring:
  profiles: docker
  cloud:
  ... #아래 옵션 추가#
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

마운트 경로에 logging file 생성 확인

```sh
$ kubectl exec -it pod/pay-7df9779d8f-vk4q9 -n wavve -- /bin/sh
$ cd /mnt/azure/logs
$ tail -n 20 -f refunded.log
$ tail -n 20 -f pay.log
```
![image](https://user-images.githubusercontent.com/82795806/123204703-9660cd80-d4f3-11eb-8682-0687962e31f9.png)

![image](https://user-images.githubusercontent.com/82795806/123204736-a4165300-d4f3-11eb-8b53-ed050b288876.png)

![image](https://user-images.githubusercontent.com/82795806/123204760-b09aab80-d4f3-11eb-89bd-2f1192be4b05.png)



## Circuit Breaker

  * 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Istio를 설치하여, anticorona namespace에 주입하여 구현함

시나리오는 예약(booking)-->백신(vaccine) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리.

- Istio 다운로드 및 PATH 추가, 설치, namespace에 istio주입

```sh
$ curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.7.1 TARGET_ARCH=x86_64 sh -
※ istio v1.7.1은 Kubernetes 1.16이상에서만 동작
```

- istio PATH 추가

```sh
$ cd istio-1.7.1
$ export PATH=$PWD/bin:$PATH
```

- istio 설치

```sh
$ istioctl install --set profile=demo --set hub=gcr.io/istio-release
※ Docker Hub Rate Limiting 우회 설정
```

- namespace에 istio주입

```sh
$ kubectl label anticorona tutorial istio-injection=enabled
```

- Virsual Service 생성 (Timeout 3초 설정)
- anticorona/booking/kubernetes/booking-istio.yaml 파일 

```yml
  apiVersion: networking.istio.io/v1alpha3
  kind: VirtualService
  metadata:
    name: vs-booking-network-rule
    namespace: anticorona
  spec:
    hosts:
    - booking
    http:
    - route:
      - destination:
          host: booking
      timeout: 3s
```	  

![image](https://user-images.githubusercontent.com/82795806/120985451-956f3280-c7b6-11eb-95a4-eb5a8c1ebce4.png)


- Booking 서비스 재배포 후 Pod에 CB 부착 확인

![image](https://user-images.githubusercontent.com/82795806/120985804-ed0d9e00-c7b6-11eb-9f13-8a961c73adc0.png)


- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
  - 동시사용자 100명, 60초 동안 실시

```sh
$ siege -c100 -t10S -v --content-type "application/json" 'http://booking:8080/bookings POST {"vaccineId":1, "vcName":"FIZER", "userId":5, "status":"BOOKED"}'
```
![image](https://user-images.githubusercontent.com/82795806/120986972-1549cc80-c7b8-11eb-83e1-7bac5a0e80ed.png)


- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 
- 약 84%정도 정상적으로 처리되었음.

***

## Autoscale (HPA)

  앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- 예약 서비스에 리소스에 대한 사용량을 정의한다.

<code>booking/kubernetes/deployment.yml</code>

```yml
  resources:
    requests:
      memory: "64Mi"
      cpu: "250m"
    limits:
      memory: "500Mi"
      cpu: "500m"
```

- 예약 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:

```sh
$ kubectl autoscale deploy booking --min=1 --max=10 --cpu-percent=15
```

![image](https://user-images.githubusercontent.com/82795806/120987663-c51f3a00-c7b8-11eb-8cc3-59d725ca2f69.png)


- CB 에서 했던 방식대로 워크로드를 걸어준다.

```sh
$ siege -c200 -t10S -v --content-type "application/json" 'http://booking:8080/bookings POST {"vaccineId":1, "vcName":"FIZER", "userId":5, "status":"BOOKED"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:

```sh
$ watch kubectl get all
```

- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

* siege 부하테스트 - 전

![image](https://user-images.githubusercontent.com/82795806/120990254-51caf780-c7bb-11eb-98a6-243b69344f12.png)

* siege 부하테스트 - 후

![image](https://user-images.githubusercontent.com/82795806/120989337-66f35680-c7ba-11eb-9b4e-b1425d4a3c2f.png)


- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 

![image](https://user-images.githubusercontent.com/82795806/120990490-93f43900-c7bb-11eb-9295-c3a0a8165ff6.png)

## Zero-Downtime deploy (Readiness Probe)

- deployment.yml에 정상 적용되어 있는 readinessProbe  
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
    - hpa 설정에 의해 target 지수 초과하여 booking scale-out 진행됨  
        ![readiness-배포중](https://user-images.githubusercontent.com/18115456/120991348-7ecbda00-c7bc-11eb-8b4d-bdb6dacad1cf.png)

    - booking이 배포되는 중,  
    정상 실행중인 booking으로의 요청은 성공(201),  
    배포중인 booking으로의 요청은 실패(503 - Service Unavailable) 확인
        ![readiness2](https://user-images.githubusercontent.com/18115456/120987386-81c4cb80-c7b8-11eb-84e7-5c00a9b1a2ff.PNG)  

- 다시 readiness 정상 적용 후, Availability 100% 확인  
![readiness4](https://user-images.githubusercontent.com/18115456/120987393-825d6200-c7b8-11eb-887e-d01519123d42.PNG)

    
## Self-healing (Liveness Probe)

- deployment.yml에 정상 적용되어 있는 livenessProbe  

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

- port 및 path 잘못된 값으로 변경 후, retry 시도 확인 (in booking 서비스)  
    - booking deploy yml 수정  
        ![selfhealing(liveness)-세팅변경](https://user-images.githubusercontent.com/18115456/120985806-ed0d9e00-c7b6-11eb-834f-ffd2c627ecf0.png)

    - retry 시도 확인  
        ![selfhealing(liveness)-restarts수](https://user-images.githubusercontent.com/18115456/120985797-ebdc7100-c7b6-11eb-8b29-fed32d4a15a3.png)  
