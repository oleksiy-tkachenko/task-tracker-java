import java.time.LocalDateTime;

public class JSONObject {
    private Integer m_id;
    private String m_description;
    private TaskStatus m_status;
    private LocalDateTime m_createdAt;
    private LocalDateTime m_updatedAt;

    JSONObject(Integer id, String description, TaskStatus status, LocalDateTime createdAt, LocalDateTime updatedAt){
        m_id = id;
        m_description = description;
        m_status = status;
        m_createdAt = createdAt;
        m_updatedAt = updatedAt;
    }


    public Integer getId() {
        return m_id;
    }

    public void setId(Integer id) {
        m_id = id;
    }

    public String getDescription() {
        return m_description;
    }

    public void setDescription(String description) {
        m_description = description;
    }

    public TaskStatus getStatus() {
        return m_status;
    }

    public void setStatus(TaskStatus status) {
        m_status = status;
    }

    public LocalDateTime getCreatedAt() {
        return m_createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        m_createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return m_updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        m_updatedAt = updatedAt;
    }
}
