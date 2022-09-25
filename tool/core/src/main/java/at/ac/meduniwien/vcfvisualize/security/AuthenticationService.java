package at.ac.meduniwien.vcfvisualize.security;

import at.ac.meduniwien.vcfvisualize.data.MySqlLoader;
import at.ac.meduniwien.vcfvisualize.model.Sample;
import at.ac.meduniwien.vcfvisualize.model.Study;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.rest.dto.TokenDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private static final String SECRET_BASE64_KEY = generateRandomString();
    private static final long TOKEN_EXPIRY_TIME = 2 * 60 * 60 * 1000;

    @Autowired
    MySqlLoader mySqlLoader;

    public AuthenticationService() {
    }

    /**
     * creates a token or null if the email password combination is invalid
     * @param email the email of the user
     * @param password the password of the user
     * @return the generated token
     */
    public TokenDTO login(String email, String password) {
        //fetch the user from the database by email
        User user = mySqlLoader.getUserByEmail(email);
        if (user == null) {
            return null;
        }

        //check if the email - hashed/salted combination exists in the database
        String hashedPassword = hashPassword(password, user.getSalt());
        boolean credentialsValid = user.getHashedPassword().equals(hashedPassword);

        if (credentialsValid && user.isActive()) {
            return buildToken(email);
        } else {
            return null;
        }
    }

    /**
     * refreshes a used token
     * @param tokenString the token string
     * @return the generated (= refreshed) token
     */
    public TokenDTO refresh(String tokenString) {
        String subject = decodeJWT(tokenString);
        if (subject == null) {
            return null;
        } else {
            return buildToken(subject);
        }
    }

    private TokenDTO buildToken(String email) {
        TokenDTO tokenDTO = new TokenDTO();

        long nowMillis = System.currentTimeMillis();
        Date expiryTime = new Date(nowMillis + TOKEN_EXPIRY_TIME);

        tokenDTO.tokenString = createJWT(email, expiryTime);
        tokenDTO.user = mySqlLoader.getUserByEmail(email).convertToDTO();
        tokenDTO.expiryTime = expiryTime;

        return tokenDTO;
    }

    private static String createJWT(String subject, Date expiration) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = Base64.getDecoder().decode(SECRET_BASE64_KEY);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setSubject(subject)
                .setExpiration(expiration)
                .signWith(signatureAlgorithm, signingKey);

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }

    /**
     * returns a user if the token is valid, returns null if not
     * @param tokenString the token
     * @return the user or null
     */
    public User getUserForToken(String tokenString) {
        String subject = decodeJWT(tokenString);
        if (subject == null) {
            return null;
        } else {
            return mySqlLoader.getUserByEmail(subject);
        }
    }

    public List<User> getAllUsers() {
        return mySqlLoader.getAllUsers();
    }

    public User createUser(String email, boolean isAdmin) {
        User userToBeCreated = new User(email);
        userToBeCreated.setSalt(generateRandomString());
        userToBeCreated.setHashedPassword("");
        userToBeCreated.setActive(false);
        userToBeCreated.setActivationCode(generateRandomString());
        userToBeCreated.setAdmin(isAdmin);
        return mySqlLoader.createUser(userToBeCreated);
    }

    public void deleteUser(long userId) {
        mySqlLoader.deleteUser(userId);
    }

    public User activateUser(String activationCode, String password) {
        User userToBeActivated = mySqlLoader.getUserByActivationCode(activationCode);

        if (userToBeActivated == null) {
            return null;
        }

        userToBeActivated.setHashedPassword(hashPassword(password, userToBeActivated.getSalt()));
        userToBeActivated.setActive(true);
        return mySqlLoader.updateUser(userToBeActivated);
    }

    public boolean emailExists(String email) {
        return mySqlLoader.getUserByEmail(email) != null;
    }

    /**
     * true iff the user has access to the sample
     * @param user the user
     * @param sampleName the sample
     * @return true iff the user has access
     */
    public boolean userCanAccessSample(User user, String sampleName) {
        List<Study> studies = mySqlLoader.getStudiesForUser(user);
        for (Study study : studies) {
            boolean studyContainsSample = mySqlLoader.getSamplesForStudy(study)
                    .stream()
                    .map(Sample::getName)
                    .collect(Collectors.toList())
                    .contains(sampleName);

            if (studyContainsSample) {
                return true;
            }
        }

        return false;
    }

    private static String decodeJWT(String jwt) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(Base64.getDecoder().decode(SECRET_BASE64_KEY))
                .parseClaimsJws(jwt).getBody();

            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private static String hashPassword(String password, String salt) {
        return DigestUtils.sha256Hex(password + salt);
    }

    private static String generateRandomString() {
        return RandomStringUtils.random(40, 0, 0, true, true, null, new SecureRandom());
    }
}

