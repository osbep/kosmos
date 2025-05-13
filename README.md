
# 🏥 Hospital Kosmos - Gestión de Citas Médicas

Este proyecto es una aplicación web desarrollada con **Spring Boot** para gestionar citas médicas en un hospital. Permite a los doctores conocer cuántas citas tienen, en qué horarios y gestionar la asignación de citas según reglas de negocio específicas.

---

## 🚀 Tecnologías Utilizadas

- Java 17  
- Spring Boot  
- Spring Data JPA  
- H2 Database (in-memory)  
- Maven  
- Postman (para pruebas)  

---

## 📦 Instrucciones de Instalación

1. Clona este repositorio o descarga el `.zip` del proyecto.  
2. Importa como proyecto Maven en tu IDE (IntelliJ, Eclipse, VS Code).  
3. Ejecuta la aplicación:  
   ```bash
   mvn spring-boot:run
   ```  
4. Accede a la consola H2 (para verificar datos):  
   ```
   http://localhost:8080/h2-console
   - JDBC URL: jdbc:h2:mem:testdb
   - User: sa
   ```

---

## 🧠 Reglas de Negocio para Agendar Cita

- ❌ No se puede agendar:
  - Una cita en el mismo consultorio a la misma hora.  
  - Una cita con el mismo doctor a la misma hora.  
  - Más de **8 citas por día** por doctor.  
  - Citas para el mismo paciente con menos de **2 horas** de diferencia el mismo día.

---

## 📬 Endpoints REST Principales

| Método | URL | Descripción |
|--------|-----|-------------|
| `POST` | `/api/citas` | Crea una nueva cita |
| `GET`  | `/api/citas` | Lista todas las citas |
| `PUT`  | `/api/citas/{id}` | Edita una cita existente |
| `DELETE` | `/api/citas/{id}` | Cancela una cita |

---

## 🧪 Pruebas con Postman

1. Importa el archivo `hospital-kosmos.postman_collection.json` en Postman.  
2. Asegúrate de tener la app corriendo en `http://localhost:8080`.  
3. Ejecuta cada solicitud desde la colección.

---

## 📁 Estructura del Proyecto

```
hospital-kosmos/
├── model/
├── repository/
├── service/
├── controller/
├── resources/
│   └── application.yml
└── HospitalKosmosApplication.java
```

---

## ✍️ Autor

**Oscar Alejandro Becerril Pérez**  
[LinkedIn](https://www.linkedin.com/in/osbep-160908265/)
