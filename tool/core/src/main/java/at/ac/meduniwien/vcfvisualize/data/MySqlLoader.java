package at.ac.meduniwien.vcfvisualize.data;

import at.ac.meduniwien.vcfvisualize.model.Sample;
import at.ac.meduniwien.vcfvisualize.model.Study;
import at.ac.meduniwien.vcfvisualize.model.User;
import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

@Service
public class MySqlLoader {

    private MysqlDataSource dataSource;

    public MySqlLoader(@Value("${database.mysql.user}") String user, @Value("${database.mysql.password}") String password, @Value("${database.mysql.host}") String host) {
        dataSource = new MysqlDataSource();
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setServerName(host);

        String databaseName = "vcfvisualizetest";
        boolean datbaseExists = databaseExists(databaseName);
        if (datbaseExists) {
            System.out.println("MySql database found");
        } else {
            createDatabase(databaseName);
        }

        dataSource.setDatabaseName(databaseName);

        if (!datbaseExists) {
            createTables();
        }
    }

    @SneakyThrows
    private boolean databaseExists(String name) {
        ResultSet rs = dataSource.getConnection().getMetaData().getCatalogs();
        while (rs.next()) {
            if (rs.getString("TABLE_CAT").equals(name)) {
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    public List<User> getAllUsers() {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM researcher");
        ResultSet resultSet = statement.executeQuery();

        List<User> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(convertResultSetToUser(resultSet));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return result;
    }

    /**
     * returns a user by their email or null if not found
     * @param email the email of the user
     * @return null or the found user
     */
    @SneakyThrows
    public User getUserByEmail(String email) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM researcher WHERE email = ? LIMIT 1");
        statement.setString(1, email);
        ResultSet resultSet = statement.executeQuery();

        User user = null;
        while (resultSet.next()) {
            user = convertResultSetToUser(resultSet);
        }

        resultSet.close();
        statement.close();
        connection.close();

        return user;
    }

    @SneakyThrows
    public User getUserByActivationCode(String activationCode) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM researcher WHERE activation_code = ? LIMIT 1");
        statement.setString(1, activationCode);
        ResultSet resultSet = statement.executeQuery();

        User user = null;
        while (resultSet.next()) {
            user = convertResultSetToUser(resultSet);
        }

        resultSet.close();
        statement.close();
        connection.close();

        return user;
    }

    @SneakyThrows
    private User convertResultSetToUser(ResultSet resultSet) {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("hashed_password"),
                resultSet.getString("salt"),
                resultSet.getBoolean("active"),
                resultSet.getString("activation_code"),
                resultSet.getBoolean("admin")
        );
    }

    /**
     * inserts the user into the database
     * @param user the user to be inserted
     * @return the user
     */
    @SneakyThrows
    public User createUser(User user) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO researcher VALUES (NULL, ?, ?, ?, ?, ?, ?)");
        statement.setString(1, user.getEmail());
        statement.setString(2, user.getHashedPassword());
        statement.setString(3, user.getSalt());
        statement.setBoolean(4, user.isActive());
        statement.setString(5, user.getActivationCode());
        statement.setBoolean(6, user.isAdmin());
        statement.execute();

        statement.close();
        connection.close();

        return getUserByEmail(user.getEmail());
    }

    /**
     * deletes a user and their rights to access samples
     * @param userId the id of the user to be deleted
     */
    @SneakyThrows
    public void deleteUser(long userId) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("DELETE FROM researcher_study WHERE researcher_id = ?");
        statement.setLong(1, userId);
        statement.execute();

        PreparedStatement statementDeleteUser = connection.prepareStatement("DELETE FROM researcher WHERE id = ?");
        statementDeleteUser.setLong(1, userId);
        statementDeleteUser.execute();
    }

    /**
     * updates a user based on id
     * @param user the new values with the id to be updated
     * @return the user
     */
    @SneakyThrows
    public User updateUser(User user) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("UPDATE researcher SET email = ?, hashed_password = ?, salt = ?, active = ?, activation_code = ?, admin = ? WHERE id = ?");
        statement.setString(1, user.getEmail());
        statement.setString(2, user.getHashedPassword());
        statement.setString(3, user.getSalt());
        statement.setBoolean(4, user.isActive());
        statement.setString(5, user.getActivationCode());
        statement.setBoolean(6, user.isAdmin());
        statement.setLong(7, user.getId());
        statement.execute();

        statement.close();
        connection.close();

        return getUserByEmail(user.getEmail());
    }

    @SneakyThrows
    public List<Sample> getAllSamples() {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM sample");
        ResultSet resultSet = statement.executeQuery();

        List<Sample> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(convertResultSetToSample(resultSet));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return result;
    }

    /**
     * inserts a sample into the database
     * @param sample the sample to be inserted
     * @return the sample
     */
    @SneakyThrows
    public Sample createSample(Sample sample) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO sample VALUES (NULL, ?, ?, ?)");
        statement.setString(1, sample.getName());
        statement.setString(2, sample.getType());
        statement.setString(3, sample.getIgvPath());
        statement.execute();

        statement.close();
        connection.close();

        return getSampleByName(sample.getName());
    }

    /**
     * deletes the sample
     * @param sampleId the id of the sample to be deleted
     */
    @SneakyThrows
    public void deleteSample(long sampleId) {
        Connection connection = dataSource.getConnection();

        PreparedStatement statementDeleteStudySample = connection.prepareStatement("DELETE FROM study_sample WHERE sample_id = ?");
        statementDeleteStudySample.setLong(1, sampleId);
        statementDeleteStudySample.execute();

        PreparedStatement statementDeleteSample = connection.prepareStatement("DELETE FROM sample WHERE id = ?");
        statementDeleteSample.setLong(1, sampleId);
        statementDeleteSample.execute();

        connection.close();
    }

    @SneakyThrows
    public Sample getSampleByName(String name) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM sample WHERE name = ? LIMIT 1");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();

        Sample sample = null;
        while (resultSet.next()) {
            sample = convertResultSetToSample(resultSet);
        }

        resultSet.close();
        statement.close();
        connection.close();

        return sample;
    }


    @SneakyThrows
    private Sample convertResultSetToSample(ResultSet resultSet) {
        return new Sample(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("type"),
                resultSet.getString("igvpath")
        );
    }

    @SneakyThrows
    public List<Study> getAllStudies() {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM study");
        ResultSet resultSet = statement.executeQuery();

        List<Study> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(convertResultSetToStudy(resultSet));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return result;
    }

    @SneakyThrows
    public Study getStudyById(long id) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM study WHERE id = ? LIMIT 1");
        statement.setLong(1, id);
        ResultSet resultSet = statement.executeQuery();

        Study study = null;
        while (resultSet.next()) {
            study = convertResultSetToStudy(resultSet);
        }

        resultSet.close();
        statement.close();
        connection.close();

        return study;
    }

    @SneakyThrows
    private Study convertResultSetToStudy(ResultSet resultSet) {
        return new Study(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("type")
        );
    }

    @SneakyThrows
    public void deleteStudy(long studyId) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statementDeleteResearcherStudy = connection.prepareStatement("DELETE FROM researcher_study WHERE study_id = ?");
        statementDeleteResearcherStudy.setLong(1, studyId);
        statementDeleteResearcherStudy.execute();

        PreparedStatement statementDeleteStudySample = connection.prepareStatement("DELETE FROM study_sample WHERE study_id = ?");
        statementDeleteStudySample.setLong(1, studyId);
        statementDeleteStudySample.execute();

        PreparedStatement statementDeleteStudy = connection.prepareStatement("DELETE FROM study WHERE id = ?");
        statementDeleteStudy.setLong(1, studyId);
        statementDeleteStudy.execute();
        connection.close();
    }

    @SneakyThrows
    public void createStudy(Study study) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO study VALUES (NULL, ?, ?)");
        statement.setString(1, study.getName());
        statement.setString(2, study.getType());
        statement.execute();

        statement.close();
        connection.close();
    }

    @SneakyThrows
    public void addSampleToStudy(Sample sample, Study study) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO study_sample VALUES (NULL, ?, ?)");
        statement.setLong(1, study.getId());
        statement.setLong(2, sample.getId());
        statement.execute();

        statement.close();
        connection.close();
    }

    /**
     * returns all studies a user has access to
     * @param user the user whose studies are to be retrieved
     * @return the studies
     */
    @SneakyThrows
    public List<Study> getStudiesForUser(User user) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT study.id as id, study.name as name, study.type as type FROM study, researcher_study WHERE researcher_study.study_id = study.id AND researcher_study.researcher_id = ?");
        statement.setLong(1, user.getId());
        ResultSet resultSet = statement.executeQuery();

        List<Study> result = new LinkedList<>();
        while (resultSet.next()) {
            Study study = new Study(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("type")
            );
            result.add(study);
        }

        resultSet.close();
        statement.close();
        connection.close();

        return result;
    }

    /**
     * returns all samples a study has
     * @param study the study whose samples are to be retrieved
     * @return the samples
     */
    @SneakyThrows
    public List<Sample> getSamplesForStudy(Study study) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT sample.id as id, sample.name as name, sample.type as type, sample.igvpath as igvpath FROM sample, study_sample WHERE study_sample.sample_id = sample.id AND study_sample.study_id = ?");
        statement.setLong(1, study.getId());
        ResultSet resultSet = statement.executeQuery();

        List<Sample> result = new LinkedList<>();
        while (resultSet.next()) {
            Sample sample = new Sample(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getString("type"),
                    resultSet.getString("igvpath")
            );
            result.add(sample);
        }

        resultSet.close();
        statement.close();
        connection.close();

        return result;
    }

    /**
     * adds access to a study for a user
     * @param study the study the user gains access to
     * @param user the user who is granted access
     */
    @SneakyThrows
    public void addStudyToUser(Study study, User user) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO researcher_study VALUES (NULL, ?, ?)");
        statement.setLong(1, user.getId());
        statement.setLong(2, study.getId());
        statement.execute();

        statement.close();
        connection.close();
    }

    /**
     * deletes access to a study for a given user
     * @param study the study to which the user should not have access anymore
     * @param user the user whose access rights are to be revoked
     */
    @SneakyThrows
    public void removeStudyFromUser(Study study, User user) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("DELETE FROM researcher_study WHERE researcher_id = ? AND study_id = ?");
        statement.setLong(1, user.getId());
        statement.setLong(2, study.getId());
        statement.execute();

        statement.close();
        connection.close();
    }

    @SneakyThrows
    public void removeSampleFromStudy(Sample sample, Study study) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("DELETE FROM study_sample WHERE study_id = ? AND sample_id = ?");
        statement.setLong(1, study.getId());
        statement.setLong(2, sample.getId());
        statement.execute();

        statement.close();
        connection.close();
    }

    /**
     * updates a user based on name
     * @param sample the new values with the name to be updated
     * @return the user
     */
    @SneakyThrows
    public void updateSample(Sample sample) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("UPDATE sample SET type = ?, igvpath = ? WHERE name = ?");
        statement.setString(1, sample.getType());
        statement.setString(2, sample.getIgvPath());
        statement.setString(3, sample.getName());
        statement.execute();

        statement.close();
        connection.close();
    }

    @SneakyThrows
    public void insertSampleMeta(String sampleId, String column, String key, String value) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO samplemeta VALUES (NULL, ?, ?, ?, ?)");
        statement.setString(1, sampleId);
        statement.setString(2, column);
        statement.setString(3, key);
        statement.setString(4, value);
        statement.execute();

        statement.close();
        connection.close();
    }

    @SneakyThrows
    public void deleteSampleMeta(String sampleId, String column, String key) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("DELETE FROM samplemeta WHERE tablename = ? AND columnname = ? AND metaname = ?");
        statement.setString(1, sampleId);
        statement.setString(2, column);
        statement.setString(3, key);
        statement.execute();

        statement.close();
        connection.close();
    }

    @SneakyThrows
    public void deleteSampleMeta(String sampleId) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("DELETE FROM samplemeta WHERE tablename = ?");
        statement.setString(1, sampleId);
        statement.execute();

        statement.close();
        connection.close();
    }

    @SneakyThrows
    public List<String> loadSampleMeta(String sampleId, String column, String key) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT value as value FROM samplemeta WHERE tablename = ? AND columnname = ? AND metaname = ?");
        statement.setString(1, sampleId);
        statement.setString(2, column);
        statement.setString(3, key);
        statement.execute();

        ResultSet resultSet = statement.executeQuery();

        List<String> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(resultSet.getString("value"));
        }

        statement.close();
        connection.close();

        return result;
    }

    private void createDatabase(String name) {
        System.out.println("Creating MySQL datbase");
        executeUpdate("CREATE DATABASE " + name);
    }

    private void createTables() {
        System.out.println("Creating MySQL tables");
        executeStatement("create table researcher(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, email VARCHAR(4095), hashed_password VARCHAR(4095), salt VARCHAR(511), active TINYINT NOT NULL, activation_code VARCHAR(4095), admin TINYINT NOT NULL);");
        executeStatement("create table study(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, name VARCHAR(4095), type VARCHAR(255));");
        executeStatement("create table sample(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, name VARCHAR(4095), type VARCHAR(255), igvpath VARCHAR(4095));");
        executeStatement("create table study_sample(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, study_id BIGINT UNSIGNED, sample_id BIGINT UNSIGNED, FOREIGN KEY (study_id)REFERENCES study (id)ON UPDATE RESTRICT ON DELETE CASCADE, FOREIGN KEY (sample_id)REFERENCES sample (id)ON UPDATE RESTRICT ON DELETE CASCADE);");
        executeStatement("create table researcher_study(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, researcher_id BIGINT UNSIGNED, study_id BIGINT UNSIGNED, FOREIGN KEY (researcher_id)REFERENCES researcher (id)ON UPDATE RESTRICT ON DELETE CASCADE, FOREIGN KEY (study_id)REFERENCES study (id)ON UPDATE RESTRICT ON DELETE CASCADE);");
        executeStatement("create table samplemeta(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, tablename VARCHAR(1023), columnname VARCHAR(1023), metaname VARCHAR(1023), value VARCHAR(8181));");
        executeStatement("insert into researcher values (null, 'changeme', '1f6e093f4ccaf5064c5377c6e137de63479c60654032f82cf5add75d73488922', 'changeme', 1, '', 1);");
        /*executeStatement("SHOW VARIABLES LIKE 'validate_password%';");
        executeStatement("SET GLOBAL validate_password.mixed_case_count = 0;");
        executeStatement("SET GLOBAL validate_password.special_char_count = 0;");
        executeStatement("SET GLOBAL validate_password.length = 6;");
        executeStatement("SET GLOBAL validate_password.number_count = 0;");
        executeStatement("SET GLOBAL validate_password.policy = 0;");
        executeStatement("FLUSH PRIVILEGES;");*/
    }

    @SneakyThrows
    private void executeStatement(String sql) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
    }

    @SneakyThrows
    private void executeUpdate(String sql) {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.executeUpdate();
    }
}
