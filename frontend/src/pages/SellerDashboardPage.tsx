import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { Modal } from '../components/ui/Modal';
import { formatCurrency, extractErrorMessage } from '../lib/utils';
import { useAuth } from '../context/AuthContext';
import type { Product, SellerDashboardSummary, NegotiationStrategy, Order, OrderStatus } from '../types';
import {
  Plus,
  ShieldCheck,
  Lock,
  Package,
  Store,
  Sliders,
  TrendingUp,
  Eye,
  Edit2,
  Trash2,
  Search,
  Filter,
  Layers,
  CheckCircle2,
  AlertTriangle,
  History,
  Truck,
  CreditCard,
  MapPin,
  ShoppingBag,
  Clock,
} from 'lucide-react';

const CATEGORIES = [
  'All Categories',
  'Electronics',
  'Audio & Acoustics',
  'Wearables',
  'Monitors & Displays',
  'Developer Tools',
];

const CONDITIONS = ['Brand New', 'Refurbished', 'Open Box - Like New', 'Used - Very Good'];

export const SellerDashboardPage: React.FC = () => {
  const { user, quickLoginAsSeller } = useAuth();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState<'listings' | 'orders' | 'policies' | 'activity'>('listings');
  const [products, setProducts] = useState<Product[]>([]);
  const [sellerOrders, setSellerOrders] = useState<Order[]>([]);
  const [dashboardSummary, setDashboardSummary] = useState<SellerDashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('All Categories');

  // Fulfilment Order Modals & Actions
  const [selectedOrderAudit, setSelectedOrderAudit] = useState<Order | null>(null);
  const [shippingModalOrder, setShippingModalOrder] = useState<Order | null>(null);
  const [shippingCarrier, setShippingCarrier] = useState('FedEx Express');
  const [shippingTracking, setShippingTracking] = useState('');
  const [shippingNotes, setShippingNotes] = useState('Dispatched from merchant warehouse');
  const [isUpdatingOrder, setIsUpdatingOrder] = useState(false);

  // Modal State for Create / Edit Product
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingProductId, setEditingProductId] = useState<string | null>(null);
  const [modalTab, setModalTab] = useState<'basic' | 'pricing' | 'specs' | 'images' | 'policy'>('basic');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [statusAlert, setStatusAlert] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Form Fields
  const [title, setTitle] = useState('');
  const [brand, setBrand] = useState('');
  const [sku, setSku] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('Electronics');
  const [condition, setCondition] = useState('Brand New');
  const [listPrice, setListPrice] = useState<number>(499);
  const [currency, setCurrency] = useState('USD');
  const [inventoryCount, setInventoryCount] = useState<number>(15);
  const [minOrderQuantity, setMinOrderQuantity] = useState<number>(1);
  const [maxOrderQuantity, setMaxOrderQuantity] = useState<number>(5);
  const [shippingCost, setShippingCost] = useState<number>(0);
  const [estimatedDeliveryDays, setEstimatedDeliveryDays] = useState<number>(3);
  const [warrantyInfo, setWarrantyInfo] = useState('12 Months Limited Warranty');
  const [returnPolicy, setReturnPolicy] = useState('30 Days Return Policy');

  // Specifications state: list of key-value pairs
  const [specsList, setSpecsList] = useState<{ key: string; value: string }[]>([
    { key: 'Color', value: 'Matte Black' },
  ]);

  // Images state
  const [imagesList, setImagesList] = useState<string[]>([
    'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80',
  ]);
  const [newImageUrl, setNewImageUrl] = useState('');

  // Negotiation Policy Fields
  const [autoNegotiate, setAutoNegotiate] = useState<boolean>(true);
  const [floorPrice, setFloorPrice] = useState<number>(399);
  const [targetPrice, setTargetPrice] = useState<number>(459);
  const [autoAcceptPriceThreshold, setAutoAcceptPriceThreshold] = useState<number>(450);
  const [maxDiscountPct, setMaxDiscountPct] = useState<number>(20);
  const [strategy, setStrategy] = useState<NegotiationStrategy>('BALANCED');
  const [maxRounds, setMaxRounds] = useState<number>(5);
  const [allowWarrantySweeteners, setAllowWarrantySweeteners] = useState<boolean>(true);
  const [maxWarrantyMonthsSweetener, setMaxWarrantyMonthsSweetener] = useState<number>(12);
  const [allowFreeShipping, setAllowFreeShipping] = useState<boolean>(true);
  const [maxDeliveryDaysCompromise, setMaxDeliveryDaysCompromise] = useState<number>(2);
  const [minimumBuyerReputation, setMinimumBuyerReputation] = useState<number>(70);

  useEffect(() => {
    fetchSellerData();
  }, [user]);

  const fetchSellerData = async () => {
    setIsLoading(true);
    try {
      const [productsRes, summaryRes, ordersRes] = await Promise.all([
        apiClient.get<{ data: Product[] }>('/catalog/seller/my-products'),
        apiClient.get<{ data: SellerDashboardSummary }>('/sellers/dashboard-summary'),
        apiClient.get<{ data: Order[] }>('/orders/seller/my-orders'),
      ]);
      setProducts(productsRes.data?.data || []);
      setDashboardSummary(summaryRes.data?.data || null);
      setSellerOrders(ordersRes.data?.data || []);
    } catch {
      // Ignored if unauthenticated or demo mode
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateOrderStatus = async (
    orderId: string,
    targetStatus: OrderStatus,
    carrier?: string,
    trackingNumber?: string,
    notes?: string
  ) => {
    setIsUpdatingOrder(true);
    try {
      const res = await apiClient.put<{ data: Order }>(`/orders/${orderId}/status`, {
        status: targetStatus,
        carrier,
        trackingNumber,
        notes,
      });
      const updatedOrder = res.data.data;
      setSellerOrders((prev) => prev.map((o) => (o.id === orderId ? updatedOrder : o)));
      if (selectedOrderAudit && selectedOrderAudit.id === orderId) {
        setSelectedOrderAudit(updatedOrder);
      }
      setStatusAlert({
        type: 'success',
        text: `Order #${updatedOrder.orderNumber} successfully transitioned to ${targetStatus}.`,
      });
      setShippingModalOrder(null);
      fetchSellerData();
    } catch (err: unknown) {
      setStatusAlert({
        type: 'error',
        text: extractErrorMessage(err, 'Failed to update order status'),
      });
    } finally {
      setIsUpdatingOrder(false);
    }
  };

  const getOrderStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'CONFIRMED':
        return <Badge variant="success">CONFIRMED</Badge>;
      case 'PROCESSING':
        return <Badge variant="primary">PROCESSING</Badge>;
      case 'SHIPPED':
        return <Badge variant="purple">SHIPPED</Badge>;
      case 'DELIVERED':
        return <Badge variant="success">DELIVERED</Badge>;
      case 'PAID':
        return <Badge variant="success">PAID</Badge>;
      case 'PAYMENT_PENDING':
        return <Badge variant="warning">PAYMENT PENDING</Badge>;
      case 'CANCELLED':
        return <Badge variant="danger">CANCELLED</Badge>;
      case 'REFUNDED':
        return <Badge variant="warning">REFUNDED</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  const openCreateModal = () => {
    setEditingProductId(null);
    setTitle('');
    setBrand('');
    setSku('');
    setDescription('');
    setCategory('Electronics');
    setCondition('Brand New');
    setListPrice(499);
    setCurrency('USD');
    setInventoryCount(15);
    setMinOrderQuantity(1);
    setMaxOrderQuantity(5);
    setShippingCost(0);
    setEstimatedDeliveryDays(3);
    setWarrantyInfo('12 Months Limited Warranty');
    setReturnPolicy('30 Days Return Policy');
    setSpecsList([{ key: 'Color', value: 'Space Gray' }]);
    setImagesList([
      'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80',
    ]);
    setAutoNegotiate(true);
    setFloorPrice(399);
    setTargetPrice(459);
    setAutoAcceptPriceThreshold(450);
    setMaxDiscountPct(20);
    setStrategy('BALANCED');
    setMaxRounds(5);
    setAllowWarrantySweeteners(true);
    setMaxWarrantyMonthsSweetener(12);
    setAllowFreeShipping(true);
    setMaxDeliveryDaysCompromise(2);
    setMinimumBuyerReputation(70);
    setModalTab('basic');
    setFormError(null);
    setIsModalOpen(true);
  };

  const openEditModal = (product: Product, startTab: 'basic' | 'policy' = 'basic') => {
    setEditingProductId(product.id);
    setTitle(product.title || '');
    setBrand(product.brand || '');
    setSku(product.sku || '');
    setDescription(product.description || '');
    setCategory(product.category || 'Electronics');
    setCondition(product.condition || 'Brand New');
    setListPrice(product.listPrice || 0);
    setCurrency(product.currency || 'USD');
    setInventoryCount(product.inventoryCount != null ? product.inventoryCount : product.availableInventory || 10);
    setMinOrderQuantity(product.minOrderQuantity || 1);
    setMaxOrderQuantity(product.maxOrderQuantity || 5);
    setShippingCost(product.shippingCost != null ? product.shippingCost : 0);
    setEstimatedDeliveryDays(product.estimatedDeliveryDays || 3);
    setWarrantyInfo(product.warrantyInfo || '12 Months Limited Warranty');
    setReturnPolicy(product.returnPolicy || '30 Days Return Policy');

    if (product.specifications && Object.keys(product.specifications).length > 0) {
      setSpecsList(
        Object.entries(product.specifications).map(([key, value]) => ({ key, value }))
      );
    } else {
      setSpecsList([{ key: 'Standard', value: 'Commercial Grade' }]);
    }

    setImagesList(product.images && product.images.length > 0 ? product.images : []);

    const policy = product.negotiationPolicy;
    if (policy) {
      setAutoNegotiate(policy.autoNegotiate);
      setFloorPrice(policy.floorPrice);
      setTargetPrice(policy.targetPrice);
      setAutoAcceptPriceThreshold(policy.autoAcceptPriceThreshold || policy.targetPrice);
      setMaxDiscountPct(policy.maxDiscountPct || 15);
      setStrategy(policy.strategy || 'BALANCED');
      setMaxRounds(policy.maxRounds || 5);
      setAllowWarrantySweeteners(policy.allowWarrantySweeteners);
      setMaxWarrantyMonthsSweetener(policy.maxWarrantyMonthsSweetener || 12);
      setAllowFreeShipping(policy.allowFreeShipping);
      setMaxDeliveryDaysCompromise(policy.maxDeliveryDaysCompromise || 2);
      setMinimumBuyerReputation(policy.minimumBuyerReputation || 70);
    } else {
      setFloorPrice(Math.round(product.listPrice * 0.8));
      setTargetPrice(Math.round(product.listPrice * 0.95));
      setAutoAcceptPriceThreshold(Math.round(product.listPrice * 0.92));
      setMaxDiscountPct(20);
    }

    setModalTab(startTab);
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleAddSpec = () => {
    setSpecsList([...specsList, { key: '', value: '' }]);
  };

  const handleRemoveSpec = (index: number) => {
    setSpecsList(specsList.filter((_, i) => i !== index));
  };

  const handleSpecChange = (index: number, field: 'key' | 'value', val: string) => {
    const updated = [...specsList];
    updated[index][field] = val;
    setSpecsList(updated);
  };

  const handleAddImage = () => {
    if (newImageUrl.trim() && !imagesList.includes(newImageUrl.trim())) {
      setImagesList([...imagesList, newImageUrl.trim()]);
      setNewImageUrl('');
    }
  };

  const handleRemoveImage = (index: number) => {
    setImagesList(imagesList.filter((_, i) => i !== index));
  };

  const handleToggleStatus = async (product: Product) => {
    try {
      const newStatus = !product.active;
      await apiClient.patch(`/catalog/${product.id}/status?active=${newStatus}`);
      setProducts(
        products.map((p) => (p.id === product.id ? { ...p, active: newStatus } : p))
      );
      setStatusAlert({
        type: 'success',
        text: `Listing "${product.title}" is now ${newStatus ? 'Active' : 'Paused'}.`,
      });
      fetchSellerData();
    } catch (err: unknown) {
      setStatusAlert({ type: 'error', text: extractErrorMessage(err, 'Failed to update listing status.') });
    }
  };

  const handleStockAdjustment = async (product: Product, delta: number) => {
    const currentStock = product.inventoryCount != null ? product.inventoryCount : product.availableInventory;
    const newStock = Math.max(0, currentStock + delta);
    try {
      await apiClient.patch(`/catalog/${product.id}/stock?count=${newStock}`);
      setProducts(
        products.map((p) =>
          p.id === product.id ? { ...p, inventoryCount: newStock, availableInventory: newStock } : p
        )
      );
      fetchSellerData();
    } catch (err: unknown) {
      setStatusAlert({ type: 'error', text: extractErrorMessage(err, 'Failed to adjust stock.') });
    }
  };

  const handleDeleteProduct = async (productId: string, title: string) => {
    if (!window.confirm(`Are you sure you want to delete listing "${title}"?`)) return;
    try {
      await apiClient.delete(`/catalog/${productId}`);
      setProducts(products.filter((p) => p.id !== productId));
      setStatusAlert({ type: 'success', text: `Product "${title}" deleted.` });
      fetchSellerData();
    } catch (err: unknown) {
      setStatusAlert({ type: 'error', text: extractErrorMessage(err, 'Failed to delete product.') });
    }
  };

  const handleSubmitProductForm = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    // Validations
    if (!title.trim()) {
      setFormError('Product title is required.');
      setModalTab('basic');
      return;
    }
    if (!description.trim() || description.trim().length < 10) {
      setFormError('Description must be at least 10 characters.');
      setModalTab('basic');
      return;
    }
    if (listPrice <= 0) {
      setFormError('Asking price must be greater than zero.');
      setModalTab('pricing');
      return;
    }
    if (floorPrice <= 0) {
      setFormError('Confidential floor price must be greater than zero.');
      setModalTab('policy');
      return;
    }
    if (floorPrice > listPrice) {
      setFormError(`Floor price ($${floorPrice}) cannot exceed asking price ($${listPrice}).`);
      setModalTab('policy');
      return;
    }
    if (targetPrice < floorPrice) {
      setFormError(`Target price ($${targetPrice}) cannot be lower than confidential floor price ($${floorPrice}).`);
      setModalTab('policy');
      return;
    }

    // Convert specs array to Record
    const specificationsRecord: Record<string, string> = {};
    specsList.forEach((item) => {
      if (item.key.trim() && item.value.trim()) {
        specificationsRecord[item.key.trim()] = item.value.trim();
      }
    });

    const negotiationPolicyPayload = {
      autoNegotiate,
      floorPrice,
      targetPrice,
      autoAcceptPriceThreshold,
      maxDiscountPct,
      strategy,
      maxRounds,
      allowWarrantySweeteners,
      maxWarrantyMonthsSweetener,
      allowFreeShipping,
      maxDeliveryDaysCompromise,
      minimumBuyerReputation,
    };

    const payload = {
      title: title.trim(),
      brand: brand.trim(),
      sku: sku.trim() || undefined,
      description: description.trim(),
      category: category.trim(),
      condition,
      listPrice,
      currency,
      inventoryCount,
      minOrderQuantity,
      maxOrderQuantity,
      shippingCost,
      estimatedDeliveryDays,
      warrantyInfo,
      returnPolicy,
      specifications: specificationsRecord,
      images: imagesList.length > 0 ? imagesList : ['https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80'],
      tags: [category.toLowerCase(), brand.toLowerCase(), condition.toLowerCase()].filter(Boolean),
      negotiationPolicy: negotiationPolicyPayload,
    };

    setIsSubmitting(true);
    try {
      if (editingProductId) {
        await apiClient.put(`/catalog/${editingProductId}`, payload);
        setStatusAlert({ type: 'success', text: `Product "${title}" updated successfully.` });
      } else {
        await apiClient.post('/catalog', payload);
        setStatusAlert({ type: 'success', text: `Product "${title}" listed and active in marketplace.` });
      }
      setIsModalOpen(false);
      fetchSellerData();
    } catch (err: unknown) {
      setFormError(extractErrorMessage(err, 'Operation failed. Please review the form fields.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  // Filter products by search and category
  const filteredProducts = products.filter((p) => {
    const matchesCategory = selectedCategory === 'All Categories' || p.category === selectedCategory;
    const q = searchQuery.toLowerCase();
    const matchesSearch =
      !q ||
      p.title?.toLowerCase().includes(q) ||
      p.brand?.toLowerCase().includes(q) ||
      p.sku?.toLowerCase().includes(q) ||
      p.description?.toLowerCase().includes(q);
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="space-y-8 max-w-7xl mx-auto py-6 px-4 sm:px-6">
      {/* Header with Store Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-charcoal-200/80">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Badge variant="purple">Merchant Portal</Badge>
            <span className="text-xs text-charcoal-400 font-medium">Autonomous Commerce Operations</span>
          </div>
          <h1 className="text-2xl font-black text-charcoal-900 tracking-tight">
            Seller Commerce & Governance Center
          </h1>
          <p className="text-xs text-charcoal-500 mt-1 max-w-2xl">
            Manage your store catalog, control confidential reserve floor limits, and govern autonomous AI deal
            compromises.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {(!user || user.role !== 'ROLE_SELLER') && (
            <Button variant="secondary" size="sm" onClick={quickLoginAsSeller}>
              Switch to Demo Seller
            </Button>
          )}
          <Button
            variant="outline"
            size="md"
            onClick={() => navigate('/seller/onboard')}
            leftIcon={<Store className="w-4 h-4 text-purple-600" />}
          >
            Store Setup
          </Button>
          <Button
            variant="primary"
            size="md"
            onClick={openCreateModal}
            leftIcon={<Plus className="w-4 h-4" />}
            className="bg-purple-600 hover:bg-purple-700 border-purple-600"
          >
            New Product Listing
          </Button>
        </div>
      </div>

      {statusAlert && (
        <Alert type={statusAlert.type} title={statusAlert.type === 'success' ? 'Listing Updated' : 'Notice'}>
          {statusAlert.text}
        </Alert>
      )}

      {/* Top KPI Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <Card className="p-4 bg-white border-charcoal-200/90 shadow-subtle">
          <div className="flex items-center justify-between text-charcoal-400 mb-1.5">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Products</span>
            <Package className="w-4 h-4 text-primary-600" />
          </div>
          <span className="text-2xl font-black text-charcoal-900">
            {dashboardSummary ? dashboardSummary.totalProducts : products.length}
          </span>
          <span className="text-[11px] text-emerald-600 font-semibold block mt-1">
            {dashboardSummary ? dashboardSummary.activeListings : products.filter((p) => p.active).length} Active Listings
          </span>
        </Card>

        <Card className="p-4 bg-white border-charcoal-200/90 shadow-subtle">
          <div className="flex items-center justify-between text-charcoal-400 mb-1.5">
            <span className="text-xs font-semibold uppercase tracking-wider">Inventory Units</span>
            <Layers className="w-4 h-4 text-purple-600" />
          </div>
          <span className="text-2xl font-black text-charcoal-900">
            {dashboardSummary
              ? dashboardSummary.totalInventory
              : products.reduce((acc, p) => acc + (p.availableInventory || 0), 0)}
          </span>
          <span className="text-[11px] text-charcoal-500 font-semibold block mt-1">
            {dashboardSummary?.lowStockCount ? `${dashboardSummary.lowStockCount} Low-stock items` : 'Stock Healthy'}
          </span>
        </Card>

        <Card className="p-4 bg-white border-charcoal-200/90 shadow-subtle">
          <div className="flex items-center justify-between text-charcoal-400 mb-1.5">
            <span className="text-xs font-semibold uppercase tracking-wider">Negotiations</span>
            <Sliders className="w-4 h-4 text-amber-600" />
          </div>
          <span className="text-2xl font-black text-charcoal-900">
            {dashboardSummary ? dashboardSummary.negotiationsReceived : 0}
          </span>
          <span className="text-[11px] text-amber-600 font-semibold block mt-1">Live Multi-Agent Inquiries</span>
        </Card>

        <Card className="p-4 bg-white border-charcoal-200/90 shadow-subtle">
          <div className="flex items-center justify-between text-charcoal-400 mb-1.5">
            <span className="text-xs font-semibold uppercase tracking-wider">Accepted Deals</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <span className="text-2xl font-black text-charcoal-900">
            {dashboardSummary ? dashboardSummary.acceptedDeals : 0}
          </span>
          <span className="text-[11px] text-emerald-600 font-semibold block mt-1">Consensus Agreements</span>
        </Card>

        <Card className="p-4 bg-white border-charcoal-200/90 shadow-subtle col-span-2 lg:col-span-1">
          <div className="flex items-center justify-between text-charcoal-400 mb-1.5">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Sales Volume</span>
            <TrendingUp className="w-4 h-4 text-emerald-600" />
          </div>
          <span className="text-2xl font-black text-charcoal-900">
            {dashboardSummary ? formatCurrency(dashboardSummary.totalSalesVolume) : '$0.00'}
          </span>
          <span className="text-[11px] text-charcoal-400 block mt-1">
            {dashboardSummary?.totalOrders ?? sellerOrders.length} Orders ({dashboardSummary?.pendingFulfilmentOrders || 0} To Fulfil)
          </span>
        </Card>
      </div>

      {/* Main Tabs Navigation */}
      <div className="flex overflow-x-auto border-b border-charcoal-200 gap-4">
        <button
          type="button"
          onClick={() => setActiveTab('listings')}
          className={`pb-3 px-2 text-xs font-bold flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'listings'
              ? 'border-purple-600 text-purple-700'
              : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          <Package className="w-4 h-4" />
          Product Listings & Inventory ({products.length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('orders')}
          className={`pb-3 px-2 text-xs font-bold flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'orders'
              ? 'border-purple-600 text-purple-700'
              : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          <Truck className="w-4 h-4 text-emerald-600" />
          Incoming Orders & Fulfilment ({sellerOrders.length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('policies')}
          className={`pb-3 px-2 text-xs font-bold flex items-center gap-2 border-b-2 transition-all whitespace-nowrap ${
            activeTab === 'policies'
              ? 'border-purple-600 text-purple-700'
              : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          <ShieldCheck className="w-4 h-4 text-purple-600" />
          Private Negotiation Policies & Margins
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('activity')}
          className={`pb-3 px-2 text-xs font-bold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'activity'
              ? 'border-purple-600 text-purple-700'
              : 'border-transparent text-charcoal-500 hover:text-charcoal-800'
          }`}
        >
          <History className="w-4 h-4 text-charcoal-400" />
          Recent Inquiries & Deal Stream
        </button>
      </div>

      {/* TAB 1: PRODUCT LISTINGS & INVENTORY */}
      {activeTab === 'listings' && (
        <div className="space-y-4">
          {/* Search & Filters */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-charcoal-200">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-charcoal-400 absolute left-3 top-2.5" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products by title, brand, SKU..."
                className="w-full pl-9 pr-3 py-1.5 text-xs rounded-xl border border-charcoal-200 focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-600"
              />
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <Filter className="w-4 h-4 text-charcoal-400" />
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="text-xs px-3 py-1.5 rounded-xl border border-charcoal-200 bg-white text-charcoal-800 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
              >
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Product Listings Table */}
          {isLoading ? (
            <div className="py-12 text-center text-xs text-charcoal-400">Loading catalog items...</div>
          ) : filteredProducts.length === 0 ? (
            <Card className="p-8 text-center space-y-3">
              <Package className="w-10 h-10 text-charcoal-300 mx-auto" />
              <h3 className="text-sm font-bold text-charcoal-700">No listings found</h3>
              <p className="text-xs text-charcoal-400 max-w-sm mx-auto">
                No products match your current search and category filters. Click below to add a new product.
              </p>
              <Button variant="primary" size="sm" onClick={openCreateModal} leftIcon={<Plus className="w-4 h-4" />}>
                Create First Listing
              </Button>
            </Card>
          ) : (
            <div className="bg-white rounded-2xl border border-charcoal-200 overflow-hidden shadow-subtle">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-charcoal-50/80 border-b border-charcoal-200 text-charcoal-500 font-semibold uppercase tracking-wider">
                    <tr>
                      <th className="py-3 px-4">Product</th>
                      <th className="py-3 px-4">Condition & SKU</th>
                      <th className="py-3 px-4">Asking Price</th>
                      <th className="py-3 px-4">Inventory Stock</th>
                      <th className="py-3 px-4">AI Negotiation</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-charcoal-100">
                    {filteredProducts.map((p) => {
                      const stock = p.inventoryCount != null ? p.inventoryCount : p.availableInventory;
                      return (
                        <tr key={p.id} className="hover:bg-charcoal-50/50 transition-colors">
                          <td className="py-3 px-4">
                            <div className="flex items-center gap-3">
                              <img
                                src={
                                  p.images && p.images.length > 0
                                    ? p.images[0]
                                    : 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80'
                                }
                                alt={p.title}
                                className="w-11 h-11 rounded-xl object-cover border border-charcoal-200 bg-charcoal-100 flex-shrink-0"
                              />
                              <div>
                                <span className="font-bold text-charcoal-900 block leading-tight">{p.title}</span>
                                <span className="text-[11px] text-charcoal-400">
                                  {p.brand ? `${p.brand} · ` : ''}
                                  {p.category}
                                </span>
                              </div>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <span className="font-semibold text-charcoal-700 block">{p.condition || 'Brand New'}</span>
                            <span className="text-[10px] text-charcoal-400 font-mono">{p.sku || 'NO-SKU'}</span>
                          </td>
                          <td className="py-3 px-4">
                            <span className="font-extrabold text-charcoal-900 text-sm">
                              {formatCurrency(p.listPrice)}
                            </span>
                            <span className="text-[10px] text-charcoal-400 block">{p.currency || 'USD'}</span>
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex items-center gap-2">
                              <button
                                type="button"
                                onClick={() => handleStockAdjustment(p, -1)}
                                className="w-5 h-5 rounded-md border border-charcoal-300 text-charcoal-600 hover:bg-charcoal-100 flex items-center justify-center font-bold text-xs"
                                title="Decrease stock"
                              >
                                -
                              </button>
                              <span
                                className={`font-bold px-2 py-0.5 rounded text-xs ${
                                  stock <= 5 ? 'bg-amber-50 text-amber-800' : 'text-charcoal-800'
                                }`}
                              >
                                {stock}
                              </span>
                              <button
                                type="button"
                                onClick={() => handleStockAdjustment(p, 1)}
                                className="w-5 h-5 rounded-md border border-charcoal-300 text-charcoal-600 hover:bg-charcoal-100 flex items-center justify-center font-bold text-xs"
                                title="Increase stock"
                              >
                                +
                              </button>
                            </div>
                            {stock <= 5 && (
                              <span className="text-[10px] text-amber-600 flex items-center gap-1 mt-0.5">
                                <AlertTriangle className="w-3 h-3" /> Low stock
                              </span>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            {p.autoNegotiate ? (
                              <Badge variant="purple" size="sm">
                                Auto Active (5 Rnds)
                              </Badge>
                            ) : (
                              <Badge variant="default" size="sm">
                                Manual / Fixed
                              </Badge>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            <button
                              type="button"
                              onClick={() => handleToggleStatus(p)}
                              className={`px-2.5 py-1 rounded-full text-[10px] font-bold border transition-colors ${
                                p.active
                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                                  : 'bg-charcoal-100 text-charcoal-500 border-charcoal-200 hover:bg-charcoal-200'
                              }`}
                            >
                              {p.active ? '● Active' : '○ Paused'}
                            </button>
                          </td>
                          <td className="py-3 px-4 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              <button
                                type="button"
                                onClick={() => openEditModal(p, 'basic')}
                                className="p-1.5 text-charcoal-500 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                                title="Edit Listing Details"
                              >
                                <Edit2 className="w-4 h-4" />
                              </button>
                              <button
                                type="button"
                                onClick={() => openEditModal(p, 'policy')}
                                className="p-1.5 text-charcoal-500 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                                title="Edit Private Negotiation Policy"
                              >
                                <Lock className="w-4 h-4" />
                              </button>
                              <button
                                type="button"
                                onClick={() => navigate(`/catalog/${p.id}`)}
                                className="p-1.5 text-charcoal-500 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                                title="Preview Public Product"
                              >
                                <Eye className="w-4 h-4" />
                              </button>
                              <button
                                type="button"
                                onClick={() => handleDeleteProduct(p.id, p.title)}
                                className="p-1.5 text-charcoal-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                                title="Delete Listing"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: NEGOTIATION POLICY GOVERNANCE */}
      {activeTab === 'policies' && (
        <div className="space-y-5">
          {/* Confidential Notice Banner */}
          <div className="p-4 bg-purple-50/70 rounded-2xl border border-purple-200 flex items-start gap-3">
            <div className="w-8 h-8 rounded-xl bg-purple-600 text-white flex items-center justify-center flex-shrink-0 mt-0.5">
              <Lock className="w-4 h-4" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-purple-900 uppercase tracking-wider">
                Confidential Margin Guardrails & Zero-Knowledge Protocol
              </h4>
              <p className="text-xs text-purple-800 mt-0.5 leading-relaxed">
                The minimum acceptable unit prices (floor limits) and auto-acceptance thresholds below are strictly
                confidential server-side secrets. TradeTwin's negotiation orchestrator enforces them as hard limits in
                backend Java code, but they are <strong>never</strong> transmitted in public SSE streams or accessible to
                buyer agents.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {products.map((p) => {
              const pol = p.negotiationPolicy;
              const floor = pol?.floorPrice || Math.round(p.listPrice * 0.8);
              const target = pol?.targetPrice || Math.round(p.listPrice * 0.95);
              const maxDiscount = pol?.maxDiscountPct || Math.round(((p.listPrice - floor) / p.listPrice) * 100);

              return (
                <Card key={p.id} className="p-5 space-y-4 border-charcoal-200">
                  <div className="flex items-start justify-between gap-2 border-b border-charcoal-100 pb-3">
                    <div>
                      <span className="text-[11px] font-bold text-purple-700 uppercase tracking-wider block">
                        {p.category}
                      </span>
                      <h3 className="text-sm font-extrabold text-charcoal-900">{p.title}</h3>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openEditModal(p, 'policy')}
                      leftIcon={<Edit2 className="w-3.5 h-3.5" />}
                      className="text-xs"
                    >
                      Configure
                    </Button>
                  </div>

                  <div className="grid grid-cols-3 gap-2 p-3 bg-charcoal-50 rounded-xl text-center">
                    <div>
                      <span className="text-[10px] text-charcoal-400 font-bold block uppercase">Asking Price</span>
                      <span className="text-sm font-extrabold text-charcoal-900">{formatCurrency(p.listPrice)}</span>
                    </div>
                    <div className="border-x border-charcoal-200">
                      <span className="text-[10px] text-purple-700 font-bold block uppercase">Private Floor</span>
                      <span className="text-sm font-extrabold text-purple-700">{formatCurrency(floor)}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-charcoal-400 font-bold block uppercase">Max Discount</span>
                      <span className="text-sm font-extrabold text-rose-600">{maxDiscount}%</span>
                    </div>
                  </div>

                  <div className="space-y-2 text-xs">
                    <div className="flex items-center justify-between text-charcoal-600">
                      <span>Strategy Profile:</span>
                      <Badge variant="purple" size="sm">
                        {pol?.strategy || 'BALANCED'}
                      </Badge>
                    </div>
                    <div className="flex items-center justify-between text-charcoal-600">
                      <span>Auto-Accept Threshold:</span>
                      <span className="font-semibold text-emerald-700">
                        {pol?.autoAcceptPriceThreshold ? formatCurrency(pol.autoAcceptPriceThreshold) : formatCurrency(target)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-charcoal-600">
                      <span>Concessions Allowed:</span>
                      <span className="font-medium text-charcoal-800">
                        {pol?.allowFreeShipping ? 'Free Shipping · ' : ''}
                        {pol?.allowWarrantySweeteners ? '+Warranty' : 'Price Only'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-charcoal-600">
                      <span>Min Buyer Reputation:</span>
                      <span className="font-semibold text-charcoal-900">
                        ★ {pol?.minimumBuyerReputation || 70} / 100
                      </span>
                    </div>
                  </div>
                </Card>
              );
            })}
          </div>
        </div>
      )}

      {/* TAB 3: RECENT ACTIVITY & DEAL STREAM */}
      {activeTab === 'activity' && (
        <Card className="p-6 space-y-4">
          <div className="border-b border-charcoal-100 pb-3">
            <h3 className="text-sm font-bold text-charcoal-900 flex items-center gap-2">
              <History className="w-4 h-4 text-purple-600" />
              Live Inquiries, Turn Activity & Order Stream
            </h3>
            <p className="text-xs text-charcoal-500 mt-0.5">
              Real-time audit log of multi-agent negotiations, consensus agreements, and customer orders.
            </p>
          </div>

          <div className="divide-y divide-charcoal-100">
            {dashboardSummary?.recentActivity && dashboardSummary.recentActivity.length > 0 ? (
              dashboardSummary.recentActivity.map((item, idx) => (
                <div key={item.id || idx} className="py-3 flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3">
                    <div
                      className={`w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5 ${
                        item.type === 'ORDER_PLACED'
                          ? 'bg-emerald-100 text-emerald-700'
                          : item.type === 'DEAL_ACCEPTED'
                          ? 'bg-purple-100 text-purple-700'
                          : 'bg-primary-100 text-primary-700'
                      }`}
                    >
                      {item.type === 'ORDER_PLACED' ? (
                        <CheckCircle2 className="w-4 h-4" />
                      ) : item.type === 'DEAL_ACCEPTED' ? (
                        <ShieldCheck className="w-4 h-4" />
                      ) : (
                        <Sliders className="w-4 h-4" />
                      )}
                    </div>
                    <div>
                      <span className="text-xs font-bold text-charcoal-900 block">{item.title}</span>
                      <p className="text-[11px] text-charcoal-500">{item.description}</p>
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    {item.amount != null && item.amount > 0 && (
                      <span className="text-xs font-extrabold text-charcoal-900 block">
                        {formatCurrency(item.amount)}
                      </span>
                    )}
                    <span className="text-[10px] text-charcoal-400">
                      {new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-8 text-center text-xs text-charcoal-400">No activity recorded yet.</div>
            )}
          </div>
        </Card>
      )}

      {/* TAB 4: INCOMING ORDERS & FULFILMENT */}
      {activeTab === 'orders' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-charcoal-200">
            <div>
              <h3 className="text-sm font-bold text-charcoal-900 flex items-center gap-2">
                <Truck className="w-4 h-4 text-emerald-600" />
                Incoming Accepted Orders & Fulfilment State Machine
              </h3>
              <p className="text-xs text-charcoal-500 mt-0.5">
                Orders issued strictly after PayPal Sandbox payment capture. Manage fulfilment steps and dispatch tracking.
              </p>
            </div>
            <div className="flex items-center gap-2 text-[11px] font-semibold text-charcoal-500 bg-charcoal-50 px-3 py-1.5 rounded-xl border border-charcoal-200">
              <Lock className="w-3.5 h-3.5 text-purple-600" />
              <span>Authorized Shipping Info Only</span>
            </div>
          </div>

          {sellerOrders.length === 0 ? (
            <Card className="p-10 text-center text-xs text-charcoal-400">
              <ShoppingBag className="w-8 h-8 text-charcoal-300 mx-auto mb-2" />
              No customer orders received yet. When a buyer confirms a negotiated deal and completes PayPal checkout, the order appears here.
            </Card>
          ) : (
            <div className="space-y-4">
              {sellerOrders.map((ord) => (
                <Card key={ord.id} className="p-5 space-y-4 shadow-subtle">
                  {/* Header Row */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-charcoal-100">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-sm text-charcoal-900">{ord.orderNumber}</span>
                        {getOrderStatusBadge(ord.orderStatus)}
                      </div>
                      <span className="text-[11px] text-charcoal-400">
                        Captured on {new Date(ord.createdAt).toLocaleDateString()} at{' '}
                        {new Date(ord.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>

                    {/* State Machine Fulfilment Buttons */}
                    <div className="flex flex-wrap items-center gap-2">
                      {(ord.orderStatus === 'CONFIRMED' || ord.orderStatus === 'PAID') && (
                        <Button
                          variant="primary"
                          size="sm"
                          disabled={isUpdatingOrder}
                          onClick={() =>
                            handleUpdateOrderStatus(ord.id, 'PROCESSING', undefined, undefined, 'Merchant started order preparation')
                          }
                        >
                          Start Processing
                        </Button>
                      )}

                      {ord.orderStatus === 'PROCESSING' && (
                        <Button
                          variant="primary"
                          size="sm"
                          disabled={isUpdatingOrder}
                          onClick={() => {
                            setShippingModalOrder(ord);
                            setShippingCarrier(ord.carrier || 'FedEx Express');
                            setShippingTracking(ord.trackingNumber || `FX-${Math.floor(100000000 + Math.random() * 900000000)}`);
                            setShippingNotes('Dispatched from fulfillment center');
                          }}
                          leftIcon={<Truck className="w-3.5 h-3.5" />}
                        >
                          Dispatch & Ship
                        </Button>
                      )}

                      {ord.orderStatus === 'SHIPPED' && (
                        <Button
                          variant="primary"
                          size="sm"
                          disabled={isUpdatingOrder}
                          onClick={() =>
                            handleUpdateOrderStatus(ord.id, 'DELIVERED', undefined, undefined, 'Carrier confirmed delivery to buyer destination')
                          }
                          leftIcon={<CheckCircle2 className="w-3.5 h-3.5" />}
                        >
                          Mark Delivered
                        </Button>
                      )}

                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setSelectedOrderAudit(ord)}
                        leftIcon={<Eye className="w-3.5 h-3.5" />}
                      >
                        Details & Audit
                      </Button>
                    </div>
                  </div>

                  {/* Main Body Grid: Product | Delivery | PayPal Payment */}
                  <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 text-xs">
                    {/* Product & Pricing */}
                    <div className="lg:col-span-4 flex items-center gap-3 p-3 rounded-xl bg-charcoal-50/70 border border-charcoal-100">
                      <div className="w-12 h-12 rounded-lg bg-charcoal-200 overflow-hidden shrink-0">
                        {ord.productImage ? (
                          <img src={ord.productImage} alt={ord.productTitle} className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-charcoal-400">
                            <Package className="w-5 h-5" />
                          </div>
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <h4 className="font-bold text-charcoal-900 truncate">{ord.productTitle || 'Purchased Item'}</h4>
                        <span className="text-[11px] text-charcoal-500 block">
                          Qty: {ord.quantity || 1} unit × {formatCurrency(ord.unitPrice || ord.finalAmount)}
                        </span>
                        <span className="font-extrabold text-emerald-600 text-xs block mt-0.5">
                          Net Price: {formatCurrency(ord.finalAmount)}
                        </span>
                      </div>
                    </div>

                    {/* Authorized Buyer Delivery Details */}
                    <div className="lg:col-span-4 p-3 rounded-xl bg-charcoal-50/70 border border-charcoal-100 space-y-1">
                      <div className="font-bold text-charcoal-900 flex items-center gap-1.5 text-[11px]">
                        <MapPin className="w-3.5 h-3.5 text-primary-600" />
                        <span>Authorized Buyer Delivery</span>
                      </div>
                      {ord.shippingDetails ? (
                        <div className="text-[11px] text-charcoal-600 leading-snug">
                          <span className="font-semibold text-charcoal-900 block">{ord.shippingDetails.fullName}</span>
                          <span className="block">{ord.shippingDetails.addressLine1}</span>
                          <span className="block">
                            {ord.shippingDetails.city}, {ord.shippingDetails.state} {ord.shippingDetails.postalCode}
                          </span>
                        </div>
                      ) : (
                        <span className="text-[11px] text-charcoal-400 italic">No shipping address recorded</span>
                      )}
                    </div>

                    {/* PayPal Payment Record & Tracking */}
                    <div className="lg:col-span-4 p-3 rounded-xl bg-charcoal-50/70 border border-charcoal-100 space-y-1 font-mono text-[10px]">
                      <div className="font-sans font-bold text-charcoal-900 flex items-center gap-1.5 text-[11px]">
                        <CreditCard className="w-3.5 h-3.5 text-[#0070BA]" />
                        <span>PayPal Capture Audit</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-charcoal-500 font-sans">Order ID:</span>
                        <span className="text-charcoal-800">{ord.payment?.paypalOrderId || 'N/A'}</span>
                      </div>
                      {ord.payment?.captureId && (
                        <div className="flex justify-between">
                          <span className="text-charcoal-500 font-sans">Capture ID:</span>
                          <span className="text-charcoal-800">{ord.payment.captureId}</span>
                        </div>
                      )}
                      {ord.trackingNumber && (
                        <div className="flex justify-between text-purple-700 pt-1 border-t border-charcoal-200/60">
                          <span className="font-sans font-semibold">{ord.carrier}:</span>
                          <span className="font-bold">{ord.trackingNumber}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* SHIPPING DISPATCH MODAL */}
      {shippingModalOrder && (
        <Modal
          isOpen={!!shippingModalOrder}
          onClose={() => setShippingModalOrder(null)}
          title={`Dispatch & Ship Order — ${shippingModalOrder.orderNumber}`}
        >
          <div className="space-y-4">
            <Alert type="info" title="Controlled Order State Machine">
              Transitioning order to <strong>SHIPPED</strong> requires carrier and tracking confirmation for buyer shipment tracking.
            </Alert>

            <Input
              label="Shipping Carrier"
              value={shippingCarrier}
              onChange={(e) => setShippingCarrier(e.target.value)}
              placeholder="e.g., FedEx Express, UPS, DHL"
            />

            <Input
              label="Tracking Number"
              value={shippingTracking}
              onChange={(e) => setShippingTracking(e.target.value)}
              placeholder="e.g., FX-123456789"
            />

            <div>
              <label className="block text-xs font-semibold text-charcoal-700 mb-1.5">Fulfilment Notes</label>
              <input
                type="text"
                value={shippingNotes}
                onChange={(e) => setShippingNotes(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl border border-charcoal-200 text-xs text-charcoal-900 focus:ring-2 focus:ring-purple-500 outline-none"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-charcoal-100">
              <Button variant="outline" size="sm" onClick={() => setShippingModalOrder(null)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                isLoading={isUpdatingOrder}
                onClick={() =>
                  handleUpdateOrderStatus(
                    shippingModalOrder.id,
                    'SHIPPED',
                    shippingCarrier,
                    shippingTracking,
                    shippingNotes
                  )
                }
                leftIcon={<Truck className="w-4 h-4" />}
              >
                Confirm Shipment Dispatch
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* ORDER AUDIT & MANAGEMENT MODAL */}
      {selectedOrderAudit && (
        <Modal
          isOpen={!!selectedOrderAudit}
          onClose={() => setSelectedOrderAudit(null)}
          title={`Merchant Order Audit — ${selectedOrderAudit.orderNumber}`}
        >
          <div className="space-y-5 text-xs text-charcoal-700">
            <div className="flex items-center justify-between pb-3 border-b border-charcoal-100">
              <div>
                <span className="text-[10px] text-charcoal-400 uppercase font-semibold">State Machine Status</span>
                <div className="mt-0.5">{getOrderStatusBadge(selectedOrderAudit.orderStatus)}</div>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-charcoal-400 uppercase font-semibold">Order Total</span>
                <div className="text-base font-black text-emerald-600">{formatCurrency(selectedOrderAudit.finalAmount)}</div>
              </div>
            </div>

            {/* State Transition History */}
            {selectedOrderAudit.statusHistory && selectedOrderAudit.statusHistory.length > 0 && (
              <div className="space-y-2">
                <h4 className="font-bold text-charcoal-900 flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-charcoal-500" />
                  Audit Trail
                </h4>
                <div className="border border-charcoal-200 rounded-xl overflow-hidden divide-y divide-charcoal-100 text-[11px]">
                  {selectedOrderAudit.statusHistory.map((hist, idx) => (
                    <div key={idx} className="p-2.5 flex items-start justify-between gap-2">
                      <div>
                        <div className="font-semibold text-charcoal-900">
                          {hist.fromStatus ? `${hist.fromStatus} → ` : ''}
                          <span className="text-purple-700">{hist.toStatus}</span>
                        </div>
                        {hist.notes && <p className="text-charcoal-500 text-[10px] mt-0.5">{hist.notes}</p>}
                      </div>
                      <span className="text-[10px] text-charcoal-400 font-mono shrink-0">
                        {new Date(hist.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Cancel / Refund Safeguard Actions */}
            {selectedOrderAudit.orderStatus !== 'CANCELLED' && selectedOrderAudit.orderStatus !== 'REFUNDED' && (
              <div className="p-3 rounded-xl bg-rose-50/70 border border-rose-200/80 space-y-2">
                <span className="font-bold text-rose-800 block">Exception Actions (Automatically Restocks Inventory)</span>
                <div className="flex items-center gap-2">
                  {(selectedOrderAudit.orderStatus === 'CONFIRMED' || selectedOrderAudit.orderStatus === 'PROCESSING') && (
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={isUpdatingOrder}
                      onClick={() =>
                        handleUpdateOrderStatus(
                          selectedOrderAudit.id,
                          'CANCELLED',
                          undefined,
                          undefined,
                          'Merchant cancelled order prior to dispatch'
                        )
                      }
                      className="text-rose-700 border-rose-300 hover:bg-rose-100 text-[11px]"
                    >
                      Cancel Order & Restock
                    </Button>
                  )}
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={isUpdatingOrder}
                    onClick={() =>
                      handleUpdateOrderStatus(
                        selectedOrderAudit.id,
                        'REFUNDED',
                        undefined,
                        undefined,
                        'Merchant issued full refund via settlement gateway'
                      )
                    }
                    className="text-amber-700 border-amber-300 hover:bg-amber-100 text-[11px]"
                  >
                    Mark Refunded & Restock
                  </Button>
                </div>
              </div>
            )}

            <div className="flex justify-end pt-2 border-t border-charcoal-100">
              <Button variant="outline" size="sm" onClick={() => setSelectedOrderAudit(null)}>
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* CREATE / EDIT PRODUCT MODAL */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingProductId ? 'Edit Product & Negotiation Policy' : 'Create New Product Listing'}
      >
        <div className="space-y-5">
          {formError && (
            <Alert type="error" title="Validation Warning">
              {formError}
            </Alert>
          )}

          {/* Modal Tabs */}
          <div className="flex border-b border-charcoal-200 gap-2 overflow-x-auto pb-1">
            {[
              { id: 'basic', label: '1. Basic Info' },
              { id: 'pricing', label: '2. Pricing & Stock' },
              { id: 'specs', label: '3. Specifications' },
              { id: 'images', label: '4. Image Gallery' },
              { id: 'policy', label: '5. AI Negotiation Policy' },
            ].map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setModalTab(tab.id as 'basic' | 'pricing' | 'specs' | 'images' | 'policy')}
                className={`py-1.5 px-2.5 text-xs font-bold rounded-lg transition-all ${
                  modalTab === tab.id
                    ? 'bg-purple-100 text-purple-900'
                    : 'text-charcoal-500 hover:text-charcoal-900 hover:bg-charcoal-100'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmitProductForm} className="space-y-4">
            {/* Modal Tab 1: Basic Info */}
            {modalTab === 'basic' && (
              <div className="space-y-3">
                <Input
                  label="Product Title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. SonicPulse Studio ANC Wireless Headphones"
                  required
                />
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    label="Brand"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    placeholder="e.g. SonicPulse"
                  />
                  <Input
                    label="SKU Code"
                    value={sku}
                    onChange={(e) => setSku(e.target.value)}
                    placeholder="e.g. SP-ANC-STU"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                      Category
                    </label>
                    <select
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl border border-charcoal-200 text-xs text-charcoal-800 bg-white"
                    >
                      {CATEGORIES.filter((c) => c !== 'All Categories').map((c) => (
                        <option key={c} value={c}>
                          {c}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                      Condition
                    </label>
                    <select
                      value={condition}
                      onChange={(e) => setCondition(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl border border-charcoal-200 text-xs text-charcoal-800 bg-white"
                    >
                      {CONDITIONS.map((cond) => (
                        <option key={cond} value={cond}>
                          {cond}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                    Description & Highlights
                  </label>
                  <textarea
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-charcoal-200 text-xs text-charcoal-800"
                    placeholder="Detailed description of features, materials, and capabilities..."
                    required
                  />
                </div>
              </div>
            )}

            {/* Modal Tab 2: Pricing & Logistics */}
            {modalTab === 'pricing' && (
              <div className="space-y-3">
                <div className="grid grid-cols-3 gap-3">
                  <Input
                    label="Asking Price ($)"
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={listPrice}
                    onChange={(e) => setListPrice(parseFloat(e.target.value) || 0)}
                    required
                  />
                  <Input
                    label="Initial Inventory"
                    type="number"
                    min="0"
                    value={inventoryCount}
                    onChange={(e) => setInventoryCount(parseInt(e.target.value, 10) || 0)}
                    required
                  />
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                      Currency
                    </label>
                    <select
                      value={currency}
                      onChange={(e) => setCurrency(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl border border-charcoal-200 text-xs text-charcoal-800 bg-white"
                    >
                      <option value="USD">USD</option>
                      <option value="EUR">EUR</option>
                      <option value="GBP">GBP</option>
                      <option value="CAD">CAD</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <Input
                    label="Min Order Qty"
                    type="number"
                    min="1"
                    value={minOrderQuantity}
                    onChange={(e) => setMinOrderQuantity(parseInt(e.target.value, 10) || 1)}
                  />
                  <Input
                    label="Max Order Qty"
                    type="number"
                    min="1"
                    value={maxOrderQuantity}
                    onChange={(e) => setMaxOrderQuantity(parseInt(e.target.value, 10) || 10)}
                  />
                  <Input
                    label="Shipping Fee ($)"
                    type="number"
                    step="0.01"
                    min="0"
                    value={shippingCost}
                    onChange={(e) => setShippingCost(parseFloat(e.target.value) || 0)}
                  />
                  <Input
                    label="Delivery (Days)"
                    type="number"
                    min="1"
                    value={estimatedDeliveryDays}
                    onChange={(e) => setEstimatedDeliveryDays(parseInt(e.target.value, 10) || 3)}
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <Input
                    label="Warranty Info"
                    value={warrantyInfo}
                    onChange={(e) => setWarrantyInfo(e.target.value)}
                    placeholder="e.g. 24 Months Manufacturer"
                  />
                  <Input
                    label="Return Policy"
                    value={returnPolicy}
                    onChange={(e) => setReturnPolicy(e.target.value)}
                    placeholder="e.g. 30 Days Free Returns"
                  />
                </div>
              </div>
            )}

            {/* Modal Tab 3: Specifications */}
            {modalTab === 'specs' && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-charcoal-600">Product Technical Specifications</span>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleAddSpec}
                    leftIcon={<Plus className="w-3.5 h-3.5" />}
                    className="text-xs"
                  >
                    Add Attribute
                  </Button>
                </div>

                <div className="space-y-2 max-h-52 overflow-y-auto pr-1">
                  {specsList.map((spec, idx) => (
                    <div key={idx} className="flex items-center gap-2">
                      <input
                        type="text"
                        placeholder="Specification Name (e.g. RAM)"
                        value={spec.key}
                        onChange={(e) => handleSpecChange(idx, 'key', e.target.value)}
                        className="w-1/2 px-2.5 py-1.5 text-xs rounded-xl border border-charcoal-200"
                      />
                      <input
                        type="text"
                        placeholder="Value (e.g. 32GB LPDDR5X)"
                        value={spec.value}
                        onChange={(e) => handleSpecChange(idx, 'value', e.target.value)}
                        className="w-1/2 px-2.5 py-1.5 text-xs rounded-xl border border-charcoal-200"
                      />
                      <button
                        type="button"
                        onClick={() => handleRemoveSpec(idx)}
                        className="p-1 text-charcoal-400 hover:text-rose-600"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Modal Tab 4: Image Gallery */}
            {modalTab === 'images' && (
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    placeholder="Enter image URL (https://...)"
                    value={newImageUrl}
                    onChange={(e) => setNewImageUrl(e.target.value)}
                    className="flex-1 px-3 py-1.5 text-xs rounded-xl border border-charcoal-200"
                  />
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={handleAddImage}
                    leftIcon={<Plus className="w-3.5 h-3.5" />}
                    className="text-xs"
                  >
                    Add Image
                  </Button>
                </div>

                <div className="grid grid-cols-3 gap-2 max-h-52 overflow-y-auto p-1">
                  {imagesList.map((img, idx) => (
                    <div key={idx} className="relative group rounded-xl border border-charcoal-200 overflow-hidden h-24">
                      <img src={img} alt="Product" className="w-full h-full object-cover" />
                      <button
                        type="button"
                        onClick={() => handleRemoveImage(idx)}
                        className="absolute top-1 right-1 bg-charcoal-900/80 text-white p-1 rounded-md opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                      {idx === 0 && (
                        <span className="absolute bottom-1 left-1 bg-purple-600 text-white text-[9px] font-bold px-1.5 py-0.5 rounded">
                          Cover
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Modal Tab 5: Private AI Negotiation Policy */}
            {modalTab === 'policy' && (
              <div className="space-y-3.5 p-3.5 bg-purple-50/50 rounded-2xl border border-purple-200">
                <div className="flex items-center gap-2">
                  <Badge variant="purple" size="sm">
                    🔒 Confidential Guardrail
                  </Badge>
                  <span className="text-[11px] text-purple-900 font-semibold">
                    Never exposed to public shoppers or buyer agents
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <Input
                    label="Confidential Floor Price ($)"
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={floorPrice}
                    onChange={(e) => setFloorPrice(parseFloat(e.target.value) || 0)}
                    required
                  />
                  <Input
                    label="Target Compromise ($)"
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={targetPrice}
                    onChange={(e) => setTargetPrice(parseFloat(e.target.value) || 0)}
                    required
                  />
                  <Input
                    label="Auto-Accept Threshold ($)"
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={autoAcceptPriceThreshold}
                    onChange={(e) => setAutoAcceptPriceThreshold(parseFloat(e.target.value) || 0)}
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                      Strategy Profile
                    </label>
                    <select
                      value={strategy}
                      onChange={(e) => setStrategy(e.target.value as NegotiationStrategy)}
                      className="w-full px-3 py-2 rounded-xl border border-charcoal-200 text-xs text-charcoal-800 bg-white"
                    >
                      <option value="CONSERVATIVE">Conservative (Strict Margin Defense)</option>
                      <option value="BALANCED">Balanced (Pareto Compromise)</option>
                      <option value="COOPERATIVE">Cooperative (Fast Volume Liquidation)</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-charcoal-700 uppercase tracking-wider mb-1.5">
                      Max Negotiation Rounds
                    </label>
                    <select
                      value={maxRounds}
                      onChange={(e) => setMaxRounds(parseInt(e.target.value, 10))}
                      className="w-full px-3 py-2 rounded-xl border border-charcoal-200 text-xs text-charcoal-800 bg-white"
                    >
                      <option value="3">3 Rounds (Rapid)</option>
                      <option value="5">5 Rounds (Standard)</option>
                      <option value="8">8 Rounds (Deep)</option>
                    </select>
                  </div>
                </div>

                <div className="flex items-center justify-between p-2.5 rounded-xl border border-purple-200 bg-white text-xs">
                  <div>
                    <span className="font-bold text-charcoal-900 block">Enable Autonomous Twin Negotiation</span>
                    <span className="text-[10px] text-charcoal-400">
                      Allows AI agent to counter buyer bids in real time
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    checked={autoNegotiate}
                    onChange={(e) => setAutoNegotiate(e.target.checked)}
                    className="w-4 h-4 text-purple-600 rounded"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs">
                  <label className="flex items-center gap-2 p-2 bg-white rounded-xl border border-charcoal-200 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={allowFreeShipping}
                      onChange={(e) => setAllowFreeShipping(e.target.checked)}
                      className="w-3.5 h-3.5 text-purple-600"
                    />
                    <span>Allow Free Shipping concession</span>
                  </label>
                  <label className="flex items-center gap-2 p-2 bg-white rounded-xl border border-charcoal-200 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={allowWarrantySweeteners}
                      onChange={(e) => setAllowWarrantySweeteners(e.target.checked)}
                      className="w-3.5 h-3.5 text-purple-600"
                    />
                    <span>Allow Warranty sweetener</span>
                  </label>
                </div>
              </div>
            )}

            <div className="flex items-center justify-between pt-3 border-t border-charcoal-200">
              <Button type="button" variant="ghost" size="sm" onClick={() => setIsModalOpen(false)}>
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="md"
                isLoading={isSubmitting}
                className="bg-purple-600 hover:bg-purple-700 border-purple-600 font-bold px-6"
              >
                {editingProductId ? 'Save Listing Changes' : 'Publish Listing'}
              </Button>
            </div>
          </form>
        </div>
      </Modal>
    </div>
  );
};
