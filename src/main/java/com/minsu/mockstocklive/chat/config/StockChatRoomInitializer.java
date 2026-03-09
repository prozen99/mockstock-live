package com.minsu.mockstocklive.chat.config;

import com.minsu.mockstocklive.chat.domain.ChatRoom;
import com.minsu.mockstocklive.chat.repository.ChatRoomRepository;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockChatRoomInitializer implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final ChatRoomRepository chatRoomRepository;

    public StockChatRoomInitializer(StockRepository stockRepository, ChatRoomRepository chatRoomRepository) {
        this.stockRepository = stockRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        stockRepository.findAllByOrderBySymbolAsc().forEach(stock -> {
            if (!chatRoomRepository.existsByStockId(stock.getId())) {
                chatRoomRepository.save(ChatRoom.create(stock, stock.getSymbol() + " chat"));
            }
        });
    }
}
