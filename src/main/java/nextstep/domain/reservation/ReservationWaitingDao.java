package nextstep.domain.reservation;

import nextstep.domain.member.Member;
import nextstep.domain.schedule.Schedule;
import nextstep.domain.theme.Theme;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Repository
public class ReservationWaitingDao {

    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_SELECT_SQL = "SELECT " +
            "reservation_waiting.id, reservation_waiting.schedule_id, reservation_waiting.member_id, " +
            "schedule.id, schedule.theme_id, schedule.date, schedule.time, " +
            "theme.id, theme.name, theme.desc, theme.price, " +
            "member.id, member.username, member.password, member.name, member.phone, member.role " +
            "from reservation_waiting " +
            "inner join schedule on reservation_waiting.schedule_id = schedule.id " +
            "inner join theme on schedule.theme_id = theme.id " +
            "inner join member on reservation_waiting.member_id = member.id ";

    public ReservationWaitingDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ReservationWaiting> rowMapper = (resultSet, rowNum) -> new ReservationWaiting(
            resultSet.getLong("reservation_waiting.id"),
            new Member(
                    resultSet.getLong("member.id"),
                    resultSet.getString("member.username"),
                    resultSet.getString("member.password"),
                    resultSet.getString("member.name"),
                    resultSet.getString("member.phone"),
                    resultSet.getString("member.role")
            ),
            new Schedule(
                    resultSet.getLong("schedule.id"),
                    new Theme(
                            resultSet.getLong("theme.id"),
                            resultSet.getString("theme.name"),
                            resultSet.getString("theme.desc"),
                            resultSet.getInt("theme.price")
                    ),
                    resultSet.getDate("schedule.date").toLocalDate(),
                    resultSet.getTime("schedule.time").toLocalTime()
            ),
            findMaxWaitNum(resultSet.getLong("schedule.id"), resultSet.getLong("reservation_waiting.id"))
    );


    public Long save(ReservationWaiting reservationWaiting) {
        String sql = "INSERT INTO reservation_waiting (member_id, schedule_id) VALUES (?, ?);";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, reservationWaiting.getMember().getId());
            ps.setLong(2, reservationWaiting.getSchedule().getId());
            return ps;

        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private int findMaxWaitNum(Long scheduleId, Long id) {
        String sql = "SELECT COUNT(id) FROM reservation_waiting WHERE schedule_id = ? AND id <= ? LIMIT 100;";

        try {
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class, scheduleId, id);
            if (Objects.isNull(result)) return 0;
            return result;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    public ReservationWaiting findById(Long id) {
        String sql = BASE_SELECT_SQL + "where reservation_waiting.id = ?;";

        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public ReservationWaiting findByScheduleId(Long scheduleId) {
        String sql = BASE_SELECT_SQL +
                "WHERE reservation_waiting.id = (SELECT MIN(id) FROM reservation_waiting WHERE schedule_id = ? ) ;";

        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, scheduleId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM reservation_waiting WHERE id = ?";

        jdbcTemplate.update(sql, id);
    }

    public List<ReservationWaiting> findReservationWaitingsByMemberId(Long memberId) {
        String sql = BASE_SELECT_SQL + "where member.id = ?;";

        try {
            return jdbcTemplate.query(sql, rowMapper, memberId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
