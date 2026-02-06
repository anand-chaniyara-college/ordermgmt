# Spring Boot API Execution Flow: User Registration

This document explains exactly what happens, step-by-step, when you hit **"SEND"** in Postman.

---

## 🛑 The Gatekeeper: `SecurityConfig.java`

**Trace:** `src/main/java/com/example/ordermgmt/config/SecurityConfig.java`

**1. Request Arrives at Port 8081**

- Before your code even runs, the request hits the **Spring Security Filter Chain**.
- It asks: _"Is this URL allowed?"_
- **Code Check:** It looks at `.requestMatchers("/api/auth/register").permitAll()`.
- **Result:** "Yes, this is a public endpoint. Let it pass!"

---

## 🛎️ The Receptionist: `RegistrationController.java`

**Trace:** `src/main/java/com/example/ordermgmt/controller/RegistrationController.java`

**2. Request Handling**

- Spring looks for a Controller that has `@RequestMapping("/api/auth")`.
- Then it looks for a method with `@PostMapping("/register")`.
- **Data Conversion:** It takes your raw JSON (`{ "email": "..." }`) and converts it into a Java Object (`RegistrationRequestDTO`).
- **Action:** It calls the Service layer: `registrationService.registerUser(request)`.
- **Analogy:** "I have a new form here, Mr. Manager (Service). Please process it."

---

## 🧠 The Brain: `RegistrationServiceImpl.java`

**Trace:** `src/main/java/com/example/ordermgmt/service/impl/RegistrationServiceImpl.java`

**3. Business Logic Execution**

- This is where the actual thinking happens.
- **Check 1:** "Does this email already exist?" -> Calls `appUserRepository.findByEmail()`.
- **Check 2:** "Does this Role exist?" -> Calls `userRoleRepository.findByRoleName()`.
- **Action:** If everything is good, it creates a `new AppUser()` object.
- **Security:** It encrypts the password using `passwordEncoder`.
- **Final Step:** It tells the Repository: "Save this user now!" via `appUserRepository.save(newUser)`.

---

## 🗄️ The Storage Manager: `AppUserRepository.java`

**Trace:** `src/main/java/com/example/ordermgmt/repository/AppUserRepository.java`

**4. Database Interaction**

- You didn't write any SQL code here, but Spring Data JPA does the magic.
- **Action:** It translates `save(newUser)` into a SQL command:
  ```sql
  INSERT INTO app_user (userid, email, password, ...) VALUES ('...', '...', '...', ...);
  ```
- It sends this SQL to the **MySQL Database**.

---

## ✅ The Return Trip (Response)

**5. Back to Service**

- The `save()` method finishes.
- The API returns `"Registration successful"`.

**6. Back to Controller**

- The Controller receives the string `"Registration successful"`.
- It wraps it in a `ResponseEntity` using the `RegistrationResponseDTO`.
- It returns HTTP Status `200 OK`.

**7. Back to Postman**

- Postman displays the JSON response:
  ```json
  {
    "message": "Registration successful"
  }
  ```

---

## Summary of the Chain

`Postman` -> `SecurityConfig` -> **`Controller`** -> **`Service`** -> **`Repository`** -> `Database`
