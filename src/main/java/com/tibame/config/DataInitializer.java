package com.tibame.config;

import com.tibame.model.entity.Category;
import com.tibame.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        long count = categoryRepository.count();
        if (count == 0) {
            log.info("初始化系統預設收支分類資料...");

            List<Category> defaultCategories = List.of(
                    // 支出分類 (EXPENSE)
                    Category.builder().userId(null).type("EXPENSE").name("飲食聚餐").iconCode("cup-hot").isSystem(true).sortOrder(10).build(),
                    Category.builder().userId(null).type("EXPENSE").name("交通出行").iconCode("airplane").isSystem(true).sortOrder(20).build(),
                    Category.builder().userId(null).type("EXPENSE").name("日常用品").iconCode("cart").isSystem(true).sortOrder(30).build(),
                    Category.builder().userId(null).type("EXPENSE").name("居住水電").iconCode("tag").isSystem(true).sortOrder(40).build(),
                    Category.builder().userId(null).type("EXPENSE").name("休閒娛樂").iconCode("film").isSystem(true).sortOrder(50).build(),
                    Category.builder().userId(null).type("EXPENSE").name("醫療保健").iconCode("tag").isSystem(true).sortOrder(60).build(),
                    Category.builder().userId(null).type("EXPENSE").name("其他支出").iconCode("tag").isSystem(true).sortOrder(99).build(),

                    // 收入分類 (INCOME)
                    Category.builder().userId(null).type("INCOME").name("薪資所得").iconCode("currency-dollar").isSystem(true).sortOrder(10).build(),
                    Category.builder().userId(null).type("INCOME").name("兼職副業").iconCode("book").isSystem(true).sortOrder(20).build(),
                    Category.builder().userId(null).type("INCOME").name("投資理財").iconCode("currency-dollar").isSystem(true).sortOrder(30).build(),
                    Category.builder().userId(null).type("INCOME").name("其他收入").iconCode("gift").isSystem(true).sortOrder(99).build()
            );

            categoryRepository.saveAll(defaultCategories);
            log.info("成功初始化 {} 筆系統預設分類", defaultCategories.size());
        }
    }
}
