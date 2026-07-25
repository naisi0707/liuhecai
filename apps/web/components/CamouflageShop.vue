<script setup lang="ts">
const { tenant } = useTenant()
const hasLines = computed(() => (tenant.value?.entryLines || []).some((l) => l.forumUrl))
</script>

<template>
  <div class="camo-shop">
    <header class="camo-shop__top">
      <div class="camo-shop__brand">Electro<span>Shop</span></div>
      <nav class="camo-shop__nav">
        <a href="#">Home</a>
        <a href="#">Hot Deals</a>
        <a href="#">Laptops</a>
        <a href="#">Smartphones</a>
        <a href="#">Cameras</a>
      </nav>
      <div class="camo-shop__cart">Cart (3)</div>
    </header>

    <section class="camo-shop__hero">
      <div>
        <h1>Laptop Collection</h1>
        <p>New Collection Up to 50% OFF</p>
        <button type="button">Shop now</button>
      </div>
    </section>

    <section class="camo-shop__grid">
      <article v-for="n in 8" :key="n" class="camo-shop__card">
        <div class="camo-shop__thumb">NEW</div>
        <h3>product name goes here</h3>
        <p class="camo-shop__price">發發發 <s>$990.00</s></p>
        <button type="button">add to cart</button>
      </article>
    </section>

    <footer class="camo-shop__foot">
      <p>1734 Stonecoal Road · About Us · Privacy Policy · Help</p>
    </footer>

    <CamouflageLineNav v-if="hasLines" class="camo-shop__nav-frame" />
  </div>
</template>

<style scoped lang="scss">
.camo-shop {
  position: relative;
  min-height: 100vh;
  background: #f5f5f5;
  color: #222;
  font-family: Arial, Helvetica, sans-serif;
}

.camo-shop__top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
}

.camo-shop__brand {
  font-size: 22px;
  font-weight: 800;
  color: #d10024;
  span { color: #111; }
}

.camo-shop__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  a {
    color: #333;
    text-decoration: none;
    font-size: 14px;
  }
}

.camo-shop__cart {
  margin-left: auto;
  font-weight: 700;
}

.camo-shop__hero {
  margin: 16px;
  padding: 40px 24px;
  background: linear-gradient(120deg, #1e3a5f, #2d6a9f);
  color: #fff;
  border-radius: 4px;
  h1 { margin: 0 0 8px; font-size: 28px; }
  p { margin: 0 0 16px; opacity: 0.9; }
  button {
    border: 0;
    background: #d10024;
    color: #fff;
    padding: 10px 18px;
    font-weight: 700;
    cursor: pointer;
  }
}

.camo-shop__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  padding: 0 16px 80px;
}

.camo-shop__card {
  background: #fff;
  border: 1px solid #eee;
  padding: 10px;
  text-align: center;
  h3 {
    margin: 8px 0 4px;
    font-size: 13px;
    font-weight: 600;
  }
  button {
    margin-top: 8px;
    border: 1px solid #ddd;
    background: #fff;
    padding: 6px 10px;
    cursor: pointer;
    font-size: 12px;
  }
}

.camo-shop__thumb {
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eceff1;
  color: #d10024;
  font-weight: 800;
}

.camo-shop__price {
  color: #d10024;
  font-weight: 700;
  s {
    color: #999;
    font-weight: 400;
    margin-left: 6px;
  }
}

.camo-shop__foot {
  padding: 16px;
  text-align: center;
  color: #777;
  font-size: 12px;
  background: #fff;
  border-top: 1px solid #eee;
}

.camo-shop__nav-frame {
  position: fixed;
  left: 50%;
  top: 18%;
  transform: translateX(-50%);
  width: min(440px, 94vw);
  z-index: 1000;
}
</style>
