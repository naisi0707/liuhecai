package com.liuhecai.draw;

import java.util.Optional;

public interface DrawSource {
    String name();

    Optional<FetchedDraw> fetchLatest(String lotteryType);
}
