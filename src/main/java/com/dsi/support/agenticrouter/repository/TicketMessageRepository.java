package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    @Query("""
            select tm
            from TicketMessage tm
            left join tm.author
            where tm.ticket.id = :ticketId
            order by tm.createdAt asc
        """)
    List<TicketMessage> findByTicketIdWithAuthorOrderByCreatedAtAsc(
        @Param("ticketId") Long ticketId
    );

    List<TicketMessage> findAllByTicket_IdOrderByCreatedAtAsc(Long ticketId);

}
