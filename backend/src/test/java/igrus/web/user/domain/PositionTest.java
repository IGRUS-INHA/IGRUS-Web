package igrus.web.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Position 도메인")
class PositionTest {

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 Position 생성 성공")
        void create_WithValidInfo_ReturnsPosition() {
            // given
            String name = "기술부";
            String imageUrl = "/images/tech.png";
            Integer displayOrder = 1;

            // when
            Position position = Position.create(name, imageUrl, displayOrder);

            // then
            assertThat(position).isNotNull();
            assertThat(position.getName()).isEqualTo(name);
            assertThat(position.getImageUrl()).isEqualTo(imageUrl);
            assertThat(position.getDisplayOrder()).isEqualTo(displayOrder);
        }

        @Test
        @DisplayName("imageUrl이 null이어도 생성 성공")
        void create_WithNullImageUrl_ReturnsPosition() {
            // given
            String name = "회장";
            String imageUrl = null;
            Integer displayOrder = 1;

            // when
            Position position = Position.create(name, imageUrl, displayOrder);

            // then
            assertThat(position).isNotNull();
            assertThat(position.getName()).isEqualTo(name);
            assertThat(position.getImageUrl()).isNull();
            assertThat(position.getDisplayOrder()).isEqualTo(displayOrder);
        }

        @Test
        @DisplayName("displayOrder가 null이어도 생성 성공")
        void create_WithNullDisplayOrder_ReturnsPosition() {
            // given
            String name = "부회장";
            String imageUrl = "/images/vice.png";
            Integer displayOrder = null;

            // when
            Position position = Position.create(name, imageUrl, displayOrder);

            // then
            assertThat(position).isNotNull();
            assertThat(position.getName()).isEqualTo(name);
            assertThat(position.getImageUrl()).isEqualTo(imageUrl);
            assertThat(position.getDisplayOrder()).isNull();
        }
    }

    @Nested
    @DisplayName("수정 메서드")
    class UpdateTest {

        @Test
        @DisplayName("updateName으로 직책명 수정 성공")
        void updateName_WithNewName_UpdatesName() {
            // given
            Position position = Position.create("기술부", "/images/tech.png", 1);
            String newName = "기술부장";

            // when
            position.updateName(newName);

            // then
            assertThat(position.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("updateImageUrl로 이미지 URL 수정 성공")
        void updateImageUrl_WithNewUrl_UpdatesImageUrl() {
            // given
            Position position = Position.create("기술부", "/images/tech.png", 1);
            String newImageUrl = "/images/tech-new.png";

            // when
            position.updateImageUrl(newImageUrl);

            // then
            assertThat(position.getImageUrl()).isEqualTo(newImageUrl);
        }

        @Test
        @DisplayName("updateImageUrl로 null 설정 가능")
        void updateImageUrl_WithNull_UpdatesImageUrlToNull() {
            // given
            Position position = Position.create("기술부", "/images/tech.png", 1);

            // when
            position.updateImageUrl(null);

            // then
            assertThat(position.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("updateDisplayOrder로 표시 순서 수정 성공")
        void updateDisplayOrder_WithNewOrder_UpdatesDisplayOrder() {
            // given
            Position position = Position.create("기술부", "/images/tech.png", 1);
            Integer newDisplayOrder = 5;

            // when
            position.updateDisplayOrder(newDisplayOrder);

            // then
            assertThat(position.getDisplayOrder()).isEqualTo(newDisplayOrder);
        }

        @Test
        @DisplayName("updateDisplayOrder로 null 설정 가능")
        void updateDisplayOrder_WithNull_UpdatesDisplayOrderToNull() {
            // given
            Position position = Position.create("기술부", "/images/tech.png", 1);

            // when
            position.updateDisplayOrder(null);

            // then
            assertThat(position.getDisplayOrder()).isNull();
        }

        @Test
        @DisplayName("update로 전체 정보 수정 성공")
        void update_WithNewInfo_UpdatesAllFields() {
            // given
            Position position = Position.create("기술부", "/images/tech.png", 1);
            String newName = "기술부장";
            String newImageUrl = "/images/tech-leader.png";
            Integer newDisplayOrder = 10;

            // when
            position.update(newName, newImageUrl, newDisplayOrder);

            // then
            assertThat(position.getName()).isEqualTo(newName);
            assertThat(position.getImageUrl()).isEqualTo(newImageUrl);
            assertThat(position.getDisplayOrder()).isEqualTo(newDisplayOrder);
        }
    }
}
